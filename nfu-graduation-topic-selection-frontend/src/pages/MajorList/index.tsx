import {
  addMajorUsingPost,
  deleteMajorUsingPost,
  getCollegeListUsingPost,
  getMajorUsingPost,
  getTopicGroupListUsingPost,
  updateMajorGroupUsingPost,
} from '@/services/topic-selection/organizationController';
import { PlusOutlined } from '@ant-design/icons';
import { ActionType, ModalForm, ProColumns, ProFormDependency, ProFormSelect, ProFormText, ProTable } from '@ant-design/pro-components';
import { Button, message, Popconfirm } from 'antd';
import { useRef } from 'react';

type MajorRow = {
  id?: number;
  majorName?: string;
  collegeId?: number;
  topicGroupId?: number;
};

export default () => {
  const actionRef = useRef<ActionType>();
  const loadColleges = async () => (await getCollegeListUsingPost({})).data || [];
  const loadAllGroups = async () =>
    (await getTopicGroupListUsingPost({ current: 1, pageSize: 100 })).data || [];
  const loadGroups = async (collegeId?: number) =>
    collegeId ? (await getTopicGroupListUsingPost({ collegeId })).data || [] : [];
  const columns: ProColumns<MajorRow>[] = [
    { title: '序号', dataIndex: 'id', valueType: 'indexBorder', width: 48 },
    { title: '专业名称', dataIndex: 'majorName' },
    { title: '所属学院', dataIndex: 'collegeId', valueType: 'select', request: loadColleges },
    { title: '所属选题组', dataIndex: 'topicGroupId', valueType: 'select', request: loadAllGroups },
    {
      title: '操作', valueType: 'option', render: (_, record, __, action) => [
        <ModalForm key="group" title={`配置 ${record.majorName} 的选题组`} trigger={<a>配置选题组</a>}
          onFinish={async (values) => {
            const response = await updateMajorGroupUsingPost({ majorId: record.id, topicGroupId: values.topicGroupId });
            if (response.code === 0) { message.success('配置成功'); action?.reload?.(); return true; }
            message.error(response.message); return false;
          }}>
          <ProFormSelect name="topicGroupId" label="选题组" initialValue={record.topicGroupId}
            request={() => loadGroups(record.collegeId)} rules={[{ required: true }]} />
        </ModalForm>,
        <Popconfirm key="delete" title="确定删除该专业吗？" onConfirm={async () => {
          const response = await deleteMajorUsingPost({ majorId: record.id });
          if (response.code === 0) { message.success('删除成功'); action?.reload?.(); } else { message.error(response.message); }
        }}><a style={{ color: '#ff4d4f' }}>删除</a></Popconfirm>,
      ],
    },
  ];

  return <ProTable<MajorRow>
    actionRef={actionRef}
    columns={columns}
    rowKey="id"
    headerTitle="专业管理"
    request={async (params) => {
      const response = await getMajorUsingPost(params);
      return { data: response.data?.records || [], total: response.data?.total || 0, success: response.code === 0 };
    }}
    toolBarRender={() => [
      <ModalForm key="add" title="添加专业" trigger={<Button type="primary"><PlusOutlined />添加专业</Button>}
        onFinish={async (values) => {
          const response = await addMajorUsingPost(values as API.MajorAddRequest);
          if (response.code === 0) { message.success('添加成功'); actionRef.current?.reload(); return true; }
          message.error(response.message); return false;
        }}>
        <ProFormText name="majorName" label="专业名称" rules={[{ required: true }]} />
        <ProFormSelect name="collegeId" label="所属学院" request={loadColleges} rules={[{ required: true }]} />
        <ProFormDependency name={['collegeId']}>
          {({ collegeId }) => <ProFormSelect name="topicGroupId" label="所属选题组"
            request={() => loadGroups(collegeId)} rules={[{ required: true }]} />}
        </ProFormDependency>
      </ModalForm>,
    ]}
  />;
};
