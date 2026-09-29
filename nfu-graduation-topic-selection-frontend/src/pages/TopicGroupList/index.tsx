import {
  addTopicGroupUsingPost,
  deleteTopicGroupUsingPost,
  getCollegeListUsingPost,
  getTopicGroupPageUsingPost,
  updateTopicGroupUsingPost,
} from '@/services/topic-selection/organizationController';
import { PlusOutlined } from '@ant-design/icons';
import { ActionType, ModalForm, ProColumns, ProFormSelect, ProFormText, ProTable } from '@ant-design/pro-components';
import { Button, message, Popconfirm } from 'antd';
import { useRef } from 'react';

type TopicGroupRow = {
  id?: number;
  collegeId?: number;
  groupName?: string;
};

export default () => {
  const actionRef = useRef<ActionType>();
  const collegeOptions = async () => {
    const response = await getCollegeListUsingPost({});
    return response.data || [];
  };
  const columns: ProColumns<TopicGroupRow>[] = [
    { title: '序号', dataIndex: 'id', valueType: 'indexBorder', width: 48 },
    { title: '选题组名称', dataIndex: 'groupName' },
    { title: '所属学院', dataIndex: 'collegeId', valueType: 'select', request: collegeOptions },
    {
      title: '操作',
      valueType: 'option',
      render: (_, record, __, action) => [
        <ModalForm key="edit" title="修改选题组" initialValues={record} trigger={<a>修改</a>}
          onFinish={async (values) => {
            const response = await updateTopicGroupUsingPost({ id: record.id, groupName: values.groupName });
            if (response.code === 0) { message.success('修改成功'); action?.reload?.(); return true; }
            message.error(response.message); return false;
          }}>
          <ProFormText name="groupName" label="选题组名称" rules={[{ required: true }]} />
        </ModalForm>,
        <Popconfirm key="delete" title="确定删除该选题组吗？" onConfirm={async () => {
          const response = await deleteTopicGroupUsingPost({ id: record.id });
          if (response.code === 0) { message.success('删除成功'); action?.reload?.(); } else { message.error(response.message); }
        }}><a style={{ color: '#ff4d4f' }}>删除</a></Popconfirm>,
      ],
    },
  ];
  return <ProTable<TopicGroupRow>
    actionRef={actionRef}
    columns={columns}
    rowKey="id"
    headerTitle="选题组管理"
    request={async (params) => {
      const response = await getTopicGroupPageUsingPost(params);
      return { data: response.data?.records || [], total: response.data?.total || 0, success: response.code === 0 };
    }}
    toolBarRender={() => [
      <ModalForm key="add" title="添加选题组" trigger={<Button type="primary"><PlusOutlined />添加选题组</Button>}
        onFinish={async (values) => {
          const response = await addTopicGroupUsingPost(values as API.TopicGroupAddRequest);
          if (response.code === 0) { message.success('添加成功'); actionRef.current?.reload(); return true; }
          message.error(response.message); return false;
        }}>
        <ProFormSelect name="collegeId" label="所属学院" request={collegeOptions} rules={[{ required: true }]} />
        <ProFormText name="groupName" label="选题组名称" rules={[{ required: true }]} />
      </ModalForm>,
    ]}
  />;
};
