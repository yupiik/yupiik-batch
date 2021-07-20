import { HomeOutlined } from '@ant-design/icons';
import { Alert, Breadcrumb, PageHeader, Skeleton } from 'antd';
import { useState } from 'react';
import { Link, useLocation, withRouter } from 'react-router-dom';
import { useJsonRpc } from '../../hooks/useJsonRpc';
import { ReportTable } from './ReportTable';

function BaseExecutions({ history, method, pageSize, showDuration, sortAttribute }) {
    const location = useLocation();
    const queryPage = new URLSearchParams(location.search || '').get('page');
    const [pagination, setPagination] = useState({ page: queryPage ? +queryPage : 0, pageSize: pageSize || 10 });
    const { loading, data, error } = useJsonRpc(method, pagination);
    if (loading) {
        return (<Skeleton active />);
    }
    if (error) {
        return (
            <div className="executions">
                <Alert message="Error" description={error.message} type="error" showIcon />
            </div>
        );
    }
    return (
        <div className="executions">
            <PageHeader title="Job Executions" />
            <ReportTable
                idRenderer={id => (<Link to={`/execution/${id}`}>{id}</Link>)}
                data={data.items}
                showDuration={showDuration}
                method={method}
                reversed={true}
                sortAttribute={sortAttribute}
                pagination={pageSize ? {
                    total: data.total,
                    current: pagination.page + 1,
                    showQuickJumper: true,
                    onChange: (page, pageSize) => {
                        setPagination({ page: Math.max(0, page - 1), pageSize });
                        history.push({
                            pathname: location.pathname,
                            search: `?${new URLSearchParams({ page }).toString()}`,
                        })
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
    return (<BaseExecutions {...props} showDuration={true} method="yupiik-batch-executions" pageSize={10} />);
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
    return (<BaseExecutions {...props} showDuration={true} sortAttribute={false} method="yupiik-batch-last-executions" />);
}
LastExecutionsView.Breadcrumb = LastExecutionsViewBreadcrumb;

export const ExecutionHistory = withRouter(Executions);
export const LastExecutions = withRouter(LastExecutionsView);
