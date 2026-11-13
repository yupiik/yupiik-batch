import { HomeOutlined } from '@ant-design/icons';
import { Alert, Breadcrumb, Select, Skeleton, Typography } from 'antd';
import { useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { fetchJsonRpc } from '../../service/fetchJsonRpc';
import { ReportTable } from './ReportTable';

function positiveInteger(value, fallback) {
    const number = Number(value);
    return Number.isSafeInteger(number) && number > 0 ? number : fallback;
}

function BaseExecutions({ method, showDuration, showSelectBatch, sortAttribute, state, dispatch }) {
    const [searchParams, setSearchParams] = useSearchParams();
    const page = positiveInteger(searchParams.get('page'), 1);
    const pageSize = positiveInteger(searchParams.get('size'), 10);
    const batch = searchParams.get('batch') || '';

    useEffect(() => {
        const params = { page: page - 1, pageSize, batch };
        const request = showSelectBatch ? [
            { jsonrpc: '2.0', id: 0, method, params },
            { jsonrpc: '2.0', id: 1, method: 'yupiik-batch-last-executions', params: {} },
        ] : params;
        return fetchJsonRpc(method, request, dispatch);
    }, [method, page, pageSize, batch, showSelectBatch, dispatch]);

    const updateQuery = values => {
        const query = new URLSearchParams(searchParams);
        Object.entries(values).forEach(([key, value]) => {
            if (value === '') {
                query.delete(key);
            } else {
                query.set(key, String(value));
            }
        });
        setSearchParams(query);
    };

    if (state[`${method}-loading-state`]) {
        return <Skeleton active />;
    }
    const error = state[`${method}-error`];
    if (error) {
        return <Alert title="Error" description={error.message} type="error" showIcon />;
    }
    const data = state[`${method}-data`];
    if (!data) {
        return <Skeleton active />;
    }
    const pageData = showSelectBatch ? data[0] : data;
    return (
        <div className="executions">
            <Typography.Title level={4}>Job Executions</Typography.Title>
            {showSelectBatch &&
                <div className="batch-selector" style={{ marginBottom: '1rem', display: 'flex', alignItems: 'center' }}>
                    <span style={{ marginRight: '1rem' }}>Batch:</span>
                    <Select
                        aria-label="Batch"
                        value={batch}
                        popupMatchSelectWidth={true}
                        style={{ width: '100%' }}
                        onChange={value => updateQuery({ batch: value, page: 1 })}
                        options={[
                            { value: '', label: 'All' },
                            ...Array.from(new Set(data[1].items.map(it => it.name)))
                                .filter(Boolean).map(name => ({ value: name, label: name })),
                        ]}
                    />
                </div>}
            <ReportTable
                idRenderer={id => <Link to={`/execution/${encodeURIComponent(id)}`}>{id}</Link>}
                data={pageData.items}
                showDuration={showDuration}
                method={method}
                reversed={true}
                sortAttribute={sortAttribute}
                pagination={showSelectBatch ? {
                    pageSize,
                    total: pageData.total,
                    current: page,
                    showQuickJumper: true,
                    onChange: (nextPage, nextSize) => updateQuery({
                        page: nextSize === pageSize ? nextPage : 1,
                        size: nextSize,
                    }),
                } : false}
            />
        </div>
    );
}

function ExecutionsBreadcrumb() {
    return <Breadcrumb items={[
        { title: <Link to="/"><HomeOutlined /></Link> },
        { title: 'Executions' },
    ]} />;
}

export function ExecutionHistory(props) {
    return <BaseExecutions {...props} showDuration={true} showSelectBatch={true} method="yupiik-batch-executions" />;
}
ExecutionHistory.Breadcrumb = ExecutionsBreadcrumb;

function LastExecutionsBreadcrumb() {
    return <Breadcrumb items={[
        { title: <Link to="/"><HomeOutlined /></Link> },
        { title: 'Executions Overview' },
    ]} />;
}

export function LastExecutions(props) {
    return <BaseExecutions {...props} showDuration={true} sortAttribute={false} method="yupiik-batch-last-executions" />;
}
LastExecutions.Breadcrumb = LastExecutionsBreadcrumb;
