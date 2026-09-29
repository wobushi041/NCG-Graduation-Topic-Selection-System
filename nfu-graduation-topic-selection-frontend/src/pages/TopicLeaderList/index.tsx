import { addUserUsingPost, deleteUserUsingPost, listUserByPageUsingPost } from '@/services/topic-selection/userController';
import { getCollegeListUsingPost, getTopicGroupListUsingPost } from '@/services/topic-selection/organizationController';
import { adminResetPassword } from '@/services/topic-selection/authController';
import { useTemporaryPasswordModal } from '@/utils/showTemporaryPasswordModal';
import { PlusOutlined } from '@ant-design/icons';
import { ActionType, ModalForm, ProColumns, ProFormDependency, ProFormSelect, ProFormText, ProTable } from '@ant-design/pro-components';
import { Button, message, Popconfirm } from 'antd';
import { useRef } from 'react';

type TopicLeaderRow = {
  id?: number;
  userAccount?: string;
  userName?: string;
  collegeId?: number;
  topicGroupId?: number;
};

export default () => {
  const actionRef = useRef<ActionType>();
  const { showTemporaryPasswordModal, temporaryPasswordModalNode } = useTemporaryPasswordModal();
  const loadColleges = async () => (await getCollegeListUsingPost({})).data || [];
  const loadAllGroups = async () =>
    (await getTopicGroupListUsingPost({ current: 1, pageSize: 100 })).data || [];
  const columns: ProColumns<TopicLeaderRow>[] = [
    { title: '序号', dataIndex: 'id', valueType: 'indexBorder', width: 48 },
    { title: '工号', dataIndex: 'userAccount' },
    { title: '姓名', dataIndex: 'userName' },
    { title: '所属学院', dataIndex: 'collegeId', valueType: 'select', request: loadColleges },
    { title: '负责选题组', dataIndex: 'topicGroupId', valueType: 'select', request: loadAllGroups },
    {
      title: '操作', valueType: 'option', render: (_, record, __, action) => [
        <Popconfirm key="delete" title="确定删除该用户吗？" onConfirm={async () => {
          const response = await deleteUserUsingPost({ userAccount: record.userAccount });
          if (response.code === 0) { message.success(response.message); action?.reload?.(); } else { message.error(response.message); }
        }}><a style={{ color: '#ff4d4f' }}>删除</a></Popconfirm>,
      ],
    },
  ];
  return <>
    <ProTable<TopicLeaderRow>
      actionRef={actionRef}
      columns={columns}
      rowKey="id"
      headerTitle="选题负责人管理"
      request={async (params) => {
        const response = await listUserByPageUsingPost({ ...params, userRole: 2 });
        return { data: response.data?.records || [], total: response.data?.total || 0, success: response.code === 0 };
      }}
      toolBarRender={() => [
        <ModalForm key="add" title="添加选题负责人账号" trigger={<Button type="primary"><PlusOutlined />添加选题负责人</Button>}
          onFinish={async (values) => {
            const response = await addUserUsingPost({ ...values, userRole: 2 } as API.UserAddRequest);
            if (response.code === 0) {
              showTemporaryPasswordModal({ title: '选题负责人账号创建成功', account: values.userAccount,
                userName: values.userName, rawMessage: response.message });
              actionRef.current?.reload(); return true;
            }
            message.error(response.message); return false;
          }}>
          <ProFormText name="userAccount" label="工号" rules={[{ required: true }]} />
          <ProFormText name="userName" label="姓名" rules={[{ required: true }]} />
          <ProFormSelect name="collegeId" label="所属学院" request={loadColleges}
            rules={[{ required: true }]} />
          <ProFormDependency name={['collegeId']}>
            {({ collegeId }) => <ProFormSelect name="topicGroupId" label="负责选题组"
              request={async () => collegeId ? (await getTopicGroupListUsingPost({ collegeId })).data || [] : []}
              rules={[{ required: true }]} />}
          </ProFormDependency>
        </ModalForm>,
        <ModalForm key="reset" title="重置账号密码" trigger={<Button>重置账号密码</Button>}
          onFinish={async (values) => {
            const response = await adminResetPassword({ account: values.userAccount, name: values.userName });
            if (response.code === 0) {
              showTemporaryPasswordModal({ title: '账号密码重置成功', account: values.userAccount,
                userName: values.userName, temporaryPassword: response.data?.temporaryPassword, rawMessage: response.message });
              return true;
            }
            message.error(response.message); return false;
          }}>
          <ProFormText name="userAccount" label="账号" rules={[{ required: true }]} />
          <ProFormText name="userName" label="姓名" rules={[{ required: true }]} />
        </ModalForm>,
      ]}
    />
    {temporaryPasswordModalNode}
  </>;
};
