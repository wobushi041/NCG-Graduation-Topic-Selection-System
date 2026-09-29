import { useState } from 'react';
import { Button, Input, message, Popconfirm, Select, Space } from 'antd';
import {
  deleteUserUsingPost,
  updateTeacherGroupQuotaUsingPost,
} from '@/services/topic-selection/userController';

type GroupQuotaItem = {
  topicGroupId: number;
  groupName: string;
  maxTopics: number;
};

// @ts-ignore
const AdjustLimitButton = ({ record, action }) => {
  const [editing, setEditing] = useState(false); // 是否显示输入框
  const [value, setValue] = useState('');        // 输入框当前值
  const [loading, setLoading] = useState(false); // 确认按钮 loading
  const [topicGroupId, setTopicGroupId] = useState<number>();

  const groupQuota = (record.groupQuota || []) as GroupQuotaItem[];

  // 点击确认时提交
  const handleConfirm = async () => {
    setLoading(true);
    try {
      const selectedGroup = groupQuota.find((item) => item.topicGroupId === topicGroupId);
      if (!selectedGroup) {
        message.error('请选择需要调整的选题组');
        return;
      }
      const numValue = Number(value);
      if (!Number.isInteger(numValue) || numValue < 0 || numValue > 20) {
        message.error('请输入0-20之间的有效整数');
        return;
      }
      if (numValue === selectedGroup.maxTopics) {
        message.info('值未修改');
      } else {
        const res = await updateTeacherGroupQuotaUsingPost({
          teacherAccount: record.userAccount,
          topicGroupId: selectedGroup.topicGroupId,
          maxTopics: numValue,
        });

        if (res.code === 0) {
          message.success(`已提交新值: ${value}`);
          action?.reload?.();
        } else {
          message.error(res.message || '提交失败');
        }
      }
      setEditing(false);
    } catch {
      message.error('提交失败');
    } finally {
      setLoading(false);
    }
  };

  // 点击“调整组选题额度”，使用列表已加载的选题组额度打开输入框
  const handleModifyClick = () => {
    if (groupQuota.length === 0) {
      message.error('该教师尚未配置选题组额度');
      return;
    }
    setTopicGroupId(groupQuota[0].topicGroupId);
    setValue(String(groupQuota[0].maxTopics));
    setEditing(true);
  };

  if (editing) {
    return (
      <Space>
        {groupQuota.length > 1 && (
          <Select
            value={topicGroupId}
            style={{ width: 160 }}
            options={groupQuota.map((item) => ({
              label: item.groupName,
              value: item.topicGroupId,
            }))}
            onChange={(nextTopicGroupId) => {
              const nextGroup = groupQuota.find((item) => item.topicGroupId === nextTopicGroupId);
              setTopicGroupId(nextTopicGroupId);
              setValue(String(nextGroup?.maxTopics ?? 0));
            }}
          />
        )}
        <Input
          value={value}
          onChange={(e) => setValue(e.target.value)}
          style={{ width: 100 }}
          placeholder="输入新值"
        />
        <Button type="primary" size="small" loading={loading} onClick={handleConfirm}>
          确认
        </Button>
        <Button size="small" onClick={() => setEditing(false)}>
          取消
        </Button>
      </Space>
    );
  }

  return (
    <Space>
      <a style={{ color: '#454be3' }} onClick={handleModifyClick}>
        <span className="desktop-only-label">调整组选题额度</span>
        <span className="mobile-only-label">调整额度</span>
      </a>
      <Popconfirm
        title="确定要删除该用户吗？"
        onConfirm={async () => {
          const res = await deleteUserUsingPost({ userAccount: record.userAccount });
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
      </Popconfirm>
    </Space>
  );
};

export { AdjustLimitButton };
