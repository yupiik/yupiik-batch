import { HomeOutlined } from '@ant-design/icons';
import { Alert, Breadcrumb, Descriptions, Skeleton, Tag, Typography } from 'antd';
import { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import extensions from '../../extensions/extensions';
import { fetchJsonRpc } from '../../service/fetchJsonRpc';
import './Execution.css';
import { duration, ReportTable } from './ReportTable';
import { getStatusColor } from './status';

function ExecutionBreadcrumb() {
    const { id } = useParams();
    return <Breadcrumb items={[
        { title: <Link to="/"><HomeOutlined /></Link> },
        { title: <Link to="/executions">Executions</Link> },
        { title: id },
    ]} />;
}

function Execution({ state, dispatch }) {
    const { id } = useParams();
    useEffect(() => fetchJsonRpc('yupiik-batch-execution', { id }, dispatch), [id, dispatch]);

    if (state['yupiik-batch-execution-loading-state']) {
        return <Skeleton active />;
    }
    const error = state['yupiik-batch-execution-error'];
    if (error) {
        return <Alert title="Error" description={error.message} type="error" showIcon />;
    }
    const data = state['yupiik-batch-execution-data'];
    if (!data) {
        return <Skeleton active />;
    }
    return (
        <div className="execution">
            <Typography.Title level={4}>Job Execution #{id}</Typography.Title>
            <Descriptions layout="horizontal" bordered column={{ xs: 1, sm: 1, md: 2 }} items={[
                { key: 'name', label: 'Name', children: data.name },
                { key: 'status', label: 'Status', children: (
                    <Tag color={getStatusColor(data.status || '-')}>
                        {(data.status || '-').toUpperCase()}
                    </Tag>
                ) },
                { key: 'started', label: 'Started', children: data.started },
                { key: 'finished', label: 'Finished', children: data.finished },
                { key: 'duration', label: 'Duration', children: duration(data) },
                { key: 'comment', label: 'Comment', children: extensions.commentWrapper(data.comment) },
            ]} />
            <div>
                <Typography.Title level={4}>Steps</Typography.Title>
                <ReportTable
                    idRenderer={id => id}
                    data={data.steps}
                    pagination={{ hideOnSinglePage: true }}
                    previousId={true}
                    showDuration={true}
                    reversed={false}
                    sortAttribute="started"
                    method="yupiik-batch-execution"
                />
            </div>
        </div>
    );
}

Execution.Breadcrumb = ExecutionBreadcrumb;

export default Execution;
