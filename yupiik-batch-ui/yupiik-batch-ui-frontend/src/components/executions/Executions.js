import { Alert, Breadcrumb, PageHeader, Skeleton } from 'antd';
import { HomeOutlined } from '@ant-design/icons';
import { Link, useLocation, withRouter } from 'react-router-dom';
import { ReportTable } from './ReportTable';
import { useJsonRpc } from '../../hooks/useJsonRpc';
import { useState } from 'react';

function Executions(props) {
    const location = useLocation();
    const queryPage = new URLSearchParams(location.search || '').get('page');
    const [pagination, setPagination] = useState({ page: queryPage ? +queryPage : 0, pageSize: 10 });
    const { loading, data, error } = useJsonRpc('yupiik-batch-executions', pagination);
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
                pagination={{
                    total: data.total,
                    current: pagination.page + 1,
                    showQuickJumper: true,
                    onChange: (page, pageSize) => {
                        setPagination({ page: Math.max(0, page - 1), pageSize });
                        props.history.push({
                            pathname: location.pathname,
                            search: `?${new URLSearchParams({ page }).toString()}`,
                        })
                    },
                }}
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

Executions.Breadcrumb = ExecutionsBreadcrumb;

export default withRouter(Executions);
