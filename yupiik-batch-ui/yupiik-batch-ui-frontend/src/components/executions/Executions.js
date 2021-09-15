import { HomeOutlined } from '@ant-design/icons';
import { Alert, Breadcrumb, PageHeader, Select, Skeleton } from 'antd';
import { useEffect } from 'react';
import { Link, useLocation, withRouter } from 'react-router-dom';
import { fetchJsonRpc } from '../../service/fetchJsonRpc';
import { ReportTable } from './ReportTable';

function forgeRequest({ method, showSelectBatch, queryPage, queryPageSize, batchName }) {
    const params = {
        page: queryPage ? +queryPage : 0,
        pageSize: queryPageSize ? +queryPageSize : 10,
        batch: batchName,
    };
    return showSelectBatch ?
        [
            {
                jsonrpc: '2.0',
                id: 0,
                method,
                params,
            },
            {   // fetch all batch names this way for now, since we bulk requests it is acceptable
                jsonrpc: '2.0',
                id: 1,
                method: 'yupiik-batch-last-executions',
                params: {},
            },
        ] :
        params;
}

function updatePaginationAndRequest({ method, showSelectBatch, queryPage, queryPageSize, batchName, dispatch }) {
    dispatch({
        type: `${method}-pagination`,
        value: {
            page: queryPage ? +queryPage : 0,
            pageSize: queryPageSize ? +queryPageSize : 10,
            batch: batchName,
        },
    });
    dispatch({
        type: `${method}-request-params`,
        value: forgeRequest({ method, showSelectBatch, queryPage, queryPageSize, batchName }),
    });
}

function BaseExecutions({ location, history, method, pageSize, showDuration, showSelectBatch, sortAttribute, state, dispatch }) {
    const pagination = state[`${method}-pagination`];
    const request = state[`${method}-request-params`];
    useEffect(() => {
        if (!pagination) {
            const queryParams = new URLSearchParams(location.search);
            const queryPage = queryParams.get('page');
            const queryPageSize = queryParams.get('size'); // partially wired only
            const batchName = queryParams.get('batch') || '';
            updatePaginationAndRequest({
                method,
                showSelectBatch,
                queryPage: queryPage && queryPage > 0 ? queryPage - 1 : queryPage,
                queryPageSize,
                batchName,
                dispatch,
            });
        }
        if (request) {
            fetchJsonRpc(method, request, dispatch);
        }
    }, [method, pagination, request, dispatch, location, showSelectBatch]);

    if (state[`${method}-loading-state`]) {
        return (<Skeleton active />);
    }

    const error = state[`${method}-error`];
    if (error) {
        return (
            <div className="executions">
                <Alert message="Error" description={error.message} type="error" showIcon />
            </div>
        );
    }

    const data = state[`${method}-data`];
    if (!data) {
        return (<Skeleton active />);
    }

    const hasBatchNames = Array.isArray(data);
    const pageData = hasBatchNames ? data[0] : data;
    return (
        <div className="executions">
            <PageHeader title="Job Executions" />
            {hasBatchNames && data.length === 2 &&
                <div className="batch-selector" style={{ marginBottom: '1rem', display: 'flex', alignItems: 'center' }}>
                    <span style={{ marginRight: '1rem' }}>Batch:</span>
                    <Select
                        defaultValue={(pagination || {}).batch || ''}
                        dropdownMatchSelectWidth={true}
                        style={{ width: '100%' }}
                        onChange={value => {
                            const safePagination = pagination || {};
                            updatePaginationAndRequest({
                                method,
                                showSelectBatch,
                                queryPage: safePagination.page || 0,
                                queryPageSize: safePagination.pageSize || 10,
                                batchName: decodeURIComponent(value),
                                dispatch,
                            });
                            const queryParams = new URLSearchParams(location.search);
                            queryParams.set('batch', value);
                            history.push({
                                pathname: location.pathname,
                                search: `?${queryParams.toString()}`,
                            });
                        }}>
                        <Select.Option value="">All</Select.Option>
                        {data[1].items
                            .map(it => it.name)
                            .map(it => (<Select.Option key={it} value={encodeURIComponent(it)}>{it}</Select.Option>))}
                    </Select>
                </div>}
            <ReportTable
                idRenderer={id => (<Link to={`/execution/${id}`}>{id}</Link>)}
                data={pageData.items}
                showDuration={showDuration}
                method={method}
                reversed={true}
                sortAttribute={sortAttribute}
                pagination={pageSize ? {
                    defaultPageSize: 10,
                    pageSize: pagination.pageSize || 10,
                    total: pageData.total,
                    current: pagination.page + 1,
                    showQuickJumper: true,
                    onChange: (page, pageSize) => {
                        const queryParams = new URLSearchParams(location.search);
                        queryParams.set('page', page);
                        queryParams.set('size', pageSize);
                        updatePaginationAndRequest({
                            method,
                            showSelectBatch,
                            queryPage: Math.max(0, page - 1),
                            queryPageSize: pageSize,
                            batchName: pagination.batch,
                            dispatch,
                        });
                        history.push({
                            pathname: location.pathname,
                            search: `?${queryParams.toString()}`,
                        });
                    },
                } : false}
            />
        </div>
    );
}

function ExecutionsBreadcrumb() {
    return (
        <Breadcrumb>
            <Breadcrumb.Item><Link to='/'><HomeOutlined /></Link></Breadcrumb.Item>
            <Breadcrumb.Item>Executions</Breadcrumb.Item>
        </Breadcrumb>
    );
}
function Executions(props) {
    const location = useLocation();
    return (
        <BaseExecutions
            {...props}
            location={location}
            showDuration={true}
            showSelectBatch={true}
            method="yupiik-batch-executions"
            pageSize={(props.state['yupiik-batch-executions-pagination'] || {}).pageSize}
        />
    );
}
Executions.Breadcrumb = ExecutionsBreadcrumb;

// LastExecutionsView has no pagination and show all latest execution per batch (name)
function LastExecutionsViewBreadcrumb() {
    return (
        <Breadcrumb>
            <Breadcrumb.Item><Link to='/'><HomeOutlined /></Link></Breadcrumb.Item>
            <Breadcrumb.Item>Executions Overview</Breadcrumb.Item>
        </Breadcrumb>
    );
}
function LastExecutionsView(props) {
    const location = useLocation();
    return (
        <BaseExecutions
            {...props}
            location={location}
            showDuration={true}
            sortAttribute={false}
            method="yupiik-batch-last-executions"
            pageSize={props.state.pageSize}
        />
    );
}
LastExecutionsView.Breadcrumb = LastExecutionsViewBreadcrumb;

export const ExecutionHistory = withRouter(Executions);
export const LastExecutions = withRouter(LastExecutionsView);
