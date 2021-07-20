import { Table, Tag } from "antd";
import extensions from '../../extensions/extensions';
import { getStatusColor } from "./status";

function reportColumns(idRenderer, previousId, showDuration) {
    return [
        ...[
            {
                title: 'ID',
                dataIndex: 'id',
                key: 'id',
                render: idRenderer,
            },
            {
                title: 'NAME',
                dataIndex: 'name',
                key: 'name'
            },
            {
                title: 'STATUS',
                dataIndex: 'status',
                key: 'status',
                render: (value, { id }) => (
                    <Tag color={getStatusColor(value || '-')} key={id}>
                        {(value || '-').toUpperCase()}
                    </Tag>
                ),
            },
        ],
        ...(showDuration ? [{
            title: 'DURATION',
            dataIndex: 'duration',
            key: 'duration'
        }] : []),
        {
            title: 'STARTED',
            dataIndex: 'started',
            key: 'started'
        },
        {
            title: 'FINISHED',
            dataIndex: 'finished',
            key: 'finished'
        },
        ...(previousId ? [{
            title: 'PREVIOUS',
            dataIndex: 'previousId',
            key: 'previousId'
        }] : []),
    ];
}

function duration({ started, finished }) {
    if (!finished || !started) {
        return '?';
    }
    let ms = new Date(finished) - new Date(started);
    const mn = Math.floor(ms / 60000);
    ms -= mn * 60000;
    const sec = Math.floor(ms / 1000);
    ms -= sec * 1000;
    return `${mn < 10 ? '0' : ''}${mn}:${sec < 10 ? '0' : ''}${sec}.${ms}`;
}

export function ReportTable({ data, idRenderer, pagination, previousId, showDuration, method, reversed, sortAttribute }) {
    let dataSource = data.map(it => ({ ...it, key: it.id }));
    if (showDuration) {
        dataSource = dataSource.map(it => ({ ...it, duration: duration(it) }))
    }
    if (sortAttribute) {
        dataSource = reversed ?
            dataSource.sort((a, b) => new Date(b[sortAttribute]) - new Date(a[sortAttribute])) :
            dataSource.sort((a, b) => new Date(a[sortAttribute]) - new Date(b[sortAttribute]));
    }

    let columns = reportColumns(idRenderer, previousId, showDuration);
    if (extensions.tableColumnsWrapper) {
        columns = extensions.tableColumnsWrapper({ columns, method });
    }

    return (<Table
        dataSource={dataSource}
        columns={columns}
        expandable={{
            expandedRowRender: record => <pre>{extensions.commentWrapper(record.comment)}</pre>,
            rowExpandable: record => record.comment,
        }}
        pagination={pagination}
    />);
}
