import { Table, Tag } from "antd";
import { getStatusColor } from "./status";
import extensions from '../../extensions/extensions';

function reportColumns(idRenderer, previousId) {
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
        ],
        ...(previousId ? [{
            title: 'Previous',
            dataIndex: 'previousId',
            key: 'previousId'
        }] : []),
    ];
}

export function ReportTable({ data, idRenderer, pagination, previousId }) {
    return (<Table
        dataSource={data.map(it => ({ ...it, key: it.id }))}
        columns={reportColumns(idRenderer, previousId)}
        expandable={{
            expandedRowRender: record => <pre>{extensions.commentWrapper(record.comment)}</pre>,
            rowExpandable: record => record.comment,
        }}
        pagination={pagination}
    />);
}
