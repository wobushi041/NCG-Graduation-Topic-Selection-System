import {
  addCollegeUsingPost,
  deleteCollegeUsingPost,
  getCollegeUsingPost,
} from '@/services/topic-selection/organizationController';
import { PlusOutlined } from '@ant-design/icons';
import { ActionType, ProColumns, ProFormText, ProTable } from '@ant-design/pro-components';
import { ModalForm } from '@ant-design/pro-components';
import {Button, message, Popconfirm} from 'antd';
import { useRef, useState } from 'react';

type GithubIssueItem = {
  id?: number;
  collegeName: string;
};

export default () => {
  const actionRef = useRef<ActionType>();
  const columns: ProColumns<GithubIssueItem>[] = [
    {
      title: '序号',
      dataIndex: 'id',
      valueType: 'indexBorder',
      width: 48,
    },
    {
      title: '学院',
      dataIndex: 'collegeName',
    },
    {
      title: '操作',
      valueType: 'option',
      key: 'option',
      render: (text, record, _, action) => [
        <Popconfirm
          key="delete"
          title="确定要删除该学院吗？"
          onConfirm={async () => {
            const res = await deleteCollegeUsingPost({ collegeId: record.id });
            if (res.code === 0) {
              message.success(res.message);
              action?.reload?.();
            } else {
              message.error(res.message);
            }
          }}
          okText="确定"
          cancelText="取消"
        >
          <a style={{ color: '#ff4d4f' }}>删除</a>
        </Popconfirm>,
      ],
    },
  ];

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [data, setData] = useState<GithubIssueItem[]>([]);

  return (
    <ProTable<GithubIssueItem>
      columns={columns}
      actionRef={actionRef}
      cardBordered
      /* eslint-disable-next-line @typescript-eslint/no-unused-vars */
      // @ts-ignore
      request={async (params = {}) => {
        try {
          const response = await getCollegeUsingPost(params);
          const data = response.data || {};
          return {
            data: data.records || [],
            total: data.total || 0,
            success: response.code === 0,
          };
        } catch (error) {
          console.error('Error fetching data:', error);
          return {
            data: [],
            total: 0,
            success: false,
          };
        }
      }}
      editable={{
        type: 'multiple',
      }}
      columnsState={{
        persistenceKey: 'pro-table-singe-demos',
        persistenceType: 'localStorage',
      }}
      rowKey="id"
      search={{
        labelWidth: 'auto',
      }}
      form={{
        syncToUrl: (values, type) => (type === 'get' ? { ...values } : values),
      }}
      pagination={{
        defaultPageSize: 10,
        showSizeChanger: true,
      }}
      dateFormatter="string"
      headerTitle="学院管理"
      toolBarRender={() => [
        // eslint-disable-next-line react/jsx-key
        <ModalForm<{
          collegeName: string;
        }>
          title="添加学院"
          trigger={
            <Button type="primary">
              <PlusOutlined />
              添加学院
            </Button>
          }
          autoFocusFirstInput
          modalProps={{
            destroyOnClose: true,
            onCancel: () => console.log('取消添加'),
          }}
          submitTimeout={2000}
          onFinish={async (values) => {
            const res = await addCollegeUsingPost(values);
            if (res.code === 0) {
              message.success(res.message);
              actionRef.current?.reload();
              return true;
            } else {
              message.error(res.message);
              return false;
            }
          }}
        >
          <ProFormText
            width="md"
            name="collegeName"
            label="学院名称"
            placeholder="请输入学院名称"
            rules={[{required: true, message: '请输入学院名称'}]}
          />
        </ModalForm>,
      ]}
    />
  );
};
