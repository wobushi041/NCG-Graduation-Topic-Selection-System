import React, { useEffect, useState } from 'react';
import { Button, message, Modal, Select, Transfer } from 'antd';
import type { TransferItem } from 'antd/es/transfer';
import {
  delCollegeConfigUsingPost,
  getCollegeConfigUsingGet,
  getCollegeListUsingPost,
  setCollegeConfigUsingPost,
} from '@/services/topic-selection/userController';

interface CollegeConfig {
  enableSelectCollegesList?: Record<string, string[]>;
}

const CollegeCrossTopicConfig: React.FC = () => {
  // 学院列表
  const [collegeList, setCollegeList] = useState<API.CollegeVO[]>([]);
  // 选中的学院
  const [selectedCollege, setSelectedCollege] = useState<string>('');
  // 穿梭框数据
  const [targetKeys, setTargetKeys] = useState<string[]>([]);
  const [mockData, setMockData] = useState<TransferItem[]>([]);
  // 当前配置
  const [collegeConfig, setCollegeConfig] = useState<CollegeConfig>({});

  // 获取所有学院列表
  const fetchCollegeList = async () => {
    try {
      const res = await getCollegeListUsingPost({});
      if (res.code === 0) {
        setCollegeList(res.data || []);
        // 初始化mockData
        const data = (res.data || []).map((college: API.CollegeVO) => ({
          key: String(college.value),
          title: college.label,
          description: `可选学院: ${college.label}`,
        }));
        setMockData(data);
      }
    } catch (error) {
      console.error('获取学院列表失败:', error);
      message.error('获取学院列表失败');
    }
  };

  // 获取跨学院配置
  const fetchCollegeConfig = async () => {
    try {
      const res = await getCollegeConfigUsingGet();
      if (res.code === 0) {
        setCollegeConfig(res.data || {});
        // 如果已选择学院，更新穿梭框目标项
        if (selectedCollege && res.data?.enableSelectCollegesList?.[selectedCollege]) {
          setTargetKeys(res.data.enableSelectCollegesList[selectedCollege].map(String));
        }
      }
    } catch (error) {
      console.error('获取跨学院配置失败:', error);
      message.error('获取跨学院配置失败');
    }
  };

  // 清理配置
  const handleClearConfig = async () => {
    Modal.confirm({
      title: '确认清理配置',
      content: '清理配置后，学生在跨选模式下将可以选择所有学院的题目，确定要执行此操作吗？',
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        try {
          const res = await delCollegeConfigUsingPost();
          if (res.code === 0) {
            message.success('配置清理成功');
            // 重新获取配置
            fetchCollegeConfig();
          } else {
            message.error(res.message || '配置清理失败');
          }
        } catch (error) {
          console.error('配置清理失败:', error);
          message.error('配置清理失败');
        }
      },
    });
  };

  // 初始化数据
  useEffect(() => {
    fetchCollegeList();
    fetchCollegeConfig();
  }, []);

  // 当选中的学院改变时，更新穿梭框目标项
  useEffect(() => {
    if (selectedCollege && collegeConfig?.enableSelectCollegesList?.[selectedCollege]) {
      setTargetKeys(collegeConfig.enableSelectCollegesList[selectedCollege].map(String));
    } else {
      setTargetKeys([]);
    }
  }, [selectedCollege, collegeConfig]);

  // 穿梭框变化处理
  const handleChange = (nextTargetKeys: React.Key[]) => {
    setTargetKeys(nextTargetKeys.map(String));
  };

  // 设置规则
  const handleSetRules = async () => {
    // 检查是否选择了学院
    if (!selectedCollege) {
      message.warning('请先选择一个学院');
      return;
    }

    try {
      // 构造配置数据
      const enableSelectCollegesList: Record<string, string[]> = {};

      // 更新当前选中学院的配置
      Object.keys(collegeConfig?.enableSelectCollegesList || {}).forEach((collegeName) => {
        if (collegeName !== selectedCollege) {
          enableSelectCollegesList[collegeName] = collegeConfig.enableSelectCollegesList![collegeName];
        }
      });
      // 更新当前选中学院的配置
      enableSelectCollegesList[selectedCollege] = targetKeys;

      const res = await setCollegeConfigUsingPost({
        enableSelectCollegesList,
      });

      if (res.code === 0) {
        message.success('设置成功');
        // 更新本地配置状态
        setCollegeConfig({ enableSelectCollegesList });
      } else {
        message.error(res.message || '设置失败');
      }
    } catch (error) {
      console.error('设置规则失败:', error);
      message.error('设置规则失败');
    }
  };

  return (
    <div style={{
      background: '#ffffff',
      borderRadius: 6,
      padding: '16px',
      marginBottom: 24,
      boxShadow: 'none',
    }}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
        {/* 选择学院下拉框和设置规则按钮 */}
        <div style={{
          background: '#ffffff',
          padding: '12px 16px',
          borderRadius: 6,
          marginBottom: 16,
          boxShadow: 'none',
          border: '1px solid #f0f0f0',
        }}>
          <div style={{
            display: 'flex',
            flexDirection: 'column',
            gap: '12px'
          }}>
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: 16,
              flex: 1,
              minWidth: 0,
              flexWrap: 'wrap'
            }}>
              <span style={{
                fontWeight: 500,
                flexShrink: 0,
                whiteSpace: 'nowrap'
              }}>配置跨选规则：</span>
              <Select
                style={{
                  flex: 1,
                  minWidth: 150
                }}
                placeholder="请选择学院"
                value={selectedCollege || undefined}
                onChange={(value) => setSelectedCollege(String(value))}
                options={collegeList.map((college) => ({
                  label: college.label,
                  value: String(college.value),
                }))}
              />
            </div>
            <div style={{
              display: 'flex',
              flexDirection: 'column',
              gap: 8,
              width: '100%'
            }}>
              <div style={{
                color: '#888888',
                fontSize: '12px',
              }}>
                <span style={{ color: '#8B0000' }}>*</span> 注意对一个学院配置空规则相当于允许该学院跨选所有学院，并且教师不受跨选限制
              </div>
              <div style={{
                display: 'flex',
                justifyContent: 'flex-end',
                gap: 16,
                flexWrap: 'wrap'
              }}>
                <Button type="primary" onClick={handleSetRules}>
                  设置规则
                </Button>
                <Button danger onClick={handleClearConfig}>
                  清理配置
                </Button>
              </div>
            </div>
          </div>
        </div>

        {/* 穿梭框 */}
        {selectedCollege && (
          <div style={{
            display: 'flex',
            flexDirection: 'column',
            gap: 16
          }}>
            <div style={{ fontWeight: 500 }}>
              配置 {collegeList.find((college) => String(college.value) === selectedCollege)?.label} 可选其他学院：
            </div>
            <Transfer
              dataSource={mockData}
              titles={['目标学院', '可选学院']}
              targetKeys={targetKeys}
              onChange={handleChange}
              render={(item) => item.title ?? ''}
              listStyle={{
                width: '100%',
                maxWidth: 300,
                height: 300,
              }}
              style={{
                display: 'flex',
                justifyContent: 'center'
              }}
            />
          </div>
        )}
      </div>
    </div>
  );
};

export default CollegeCrossTopicConfig;
