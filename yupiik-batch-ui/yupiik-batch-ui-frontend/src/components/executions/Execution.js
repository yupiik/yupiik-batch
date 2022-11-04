import { HomeOutlined } from '@ant-design/icons';
import { Alert, Breadcrumb, Descriptions, PageHeader, Skeleton, Tag } from 'antd';
import { useEffect, useState } from 'react';
import { useParams } from 'react-router';
import { Link } from 'react-router-dom';
import extensions from '../../extensions/extensions';
import { fetchJsonRpc } from '../../service/fetchJsonRpc';
import './Execution.css';
import {duration, ReportTable} from './ReportTable';
import { getStatusColor } from './status';

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

function Execution(props) {
    const { id } = useParams();
    const [idValue] = useState({ id });
    const {state, dispatch}= props;
    useEffect(() => fetchJsonRpc('yupiik-batch-execution', idValue, dispatch), [idValue,dispatch]);

    if (state['yupiik-batch-execution-loading-state']) {
        return (<Skeleton active />);
    }

    const error = state['yupiik-batch-execution-error'];
    if (error) {
        return (
            <div className="execution">
                <Alert message="Error" description={error.message} type="error" showIcon />
            </div>
        );
    }

    const data = state['yupiik-batch-execution-data'];
    if (!data) {
        return (<Skeleton active />);
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
                <Descriptions.Item label="Duration">{duration(data)}</Descriptions.Item>
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
