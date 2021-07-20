import { Link } from 'react-router-dom';
import { Alert, Breadcrumb, Descriptions, PageHeader, Skeleton, Tag } from 'antd';
import { HomeOutlined } from '@ant-design/icons';
import { useParams } from 'react-router';
import { ReportTable } from './ReportTable';
import { getStatusColor } from './status';
import { useJsonRpc } from '../../hooks/useJsonRpc';
import extensions from '../../extensions/extensions';

import './Execution.css';
import { useState } from 'react';

function toDuration(job) {
    try {
        const ms = new Date(Date.parse(job.finished) - Date.parse(job.started));
        return [
            `0${ms.getHours() - 1}`.slice(-2),
            `0${ms.getMinutes()}`.slice(-2),
            `0${ms.getSeconds()}`.slice(-2),
        ].join(':');
    } catch (e) {
        return "?";
    }
}

function ExecutionBreadcrumb() {
    const { id } = useParams();
    return (
        <Breadcrumb>
            <Breadcrumb.Item><Link to="/"><HomeOutlined /></Link></Breadcrumb.Item>
            <Breadcrumb.Item><Link to="/executions">Executions</Link></Breadcrumb.Item>
            <Breadcrumb.Item>{id}</Breadcrumb.Item>
        </Breadcrumb>
    );
}

function Execution() {
    const { id } = useParams();
    const [idValue] = useState({ id });
    const { loading, data, error } = useJsonRpc('yupiik-batch-execution', idValue);
    if (loading) {
        return (<Skeleton active />);
    }
    if (error) {
        return (
            <div className="execution">
                <Alert message="Error" description={error.message} type="error" showIcon />
            </div>
        );
    }
    return (
        <div className="execution">
            <PageHeader title={`Job Execution #${id}`} />
            <Descriptions layout="horizontal" bordered column={2}>
                <Descriptions.Item label="Name">{data.name}</Descriptions.Item>
                <Descriptions.Item label="Status">
                    <Tag color={getStatusColor(data.status || '-')}>
                        {(data.status || '-').toUpperCase()}
                    </Tag>
                </Descriptions.Item>
                <Descriptions.Item label="Started">{data.started}</Descriptions.Item>
                <Descriptions.Item label="Finished">{data.finished}</Descriptions.Item>
                <Descriptions.Item label="Duration">{toDuration(data)}</Descriptions.Item>
                <Descriptions.Item label="Comment">{extensions.commentWrapper(data.comment)}</Descriptions.Item>
            </Descriptions>
            <div>
                <PageHeader title="Steps" />
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
