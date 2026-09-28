import {CopyOutlined} from '@ant-design/icons';
import {ModalForm, ProFormText} from '@ant-design/pro-components';
import {Alert, Typography, message} from 'antd';
import React, {useCallback, useState} from 'react';

export interface TemporaryPasswordModalOptions {
  title?: string;
  account?: string;
  userName?: string;
  temporaryPassword?: string;
  rawMessage?: string;
}

/**
 * 从后端返回的消息（例如："成功；临时密码（仅显示一次）：xxxx"）中提取临时密码
 */
export const extractTemporaryPassword = (rawMessage?: string): string => {
  if (!rawMessage) {
    return '';
  }
  const match = rawMessage.match(/临时密码[^：:]*[：:]\s*(.+)$/);
  if (match && match[1]) {
    return match[1].trim();
  }
  return rawMessage.trim();
};

/**
 * 复制文本到剪贴板（兼容移动端与非 HTTPS 环境）
 */
export const copyToClipboard = async (text: string, successText: string) => {
  try {
    if (navigator?.clipboard?.writeText) {
      await navigator.clipboard.writeText(text);
      message.success(successText);
      return;
    }
  } catch {
    // 降级使用 textarea 复制
  }
  const textarea = document.createElement('textarea');
  textarea.value = text;
  textarea.style.position = 'fixed';
  textarea.style.opacity = '0';
  document.body.appendChild(textarea);
  textarea.select();
  const copied = document.execCommand('copy');
  document.body.removeChild(textarea);
  if (copied) {
    message.success(successText);
  } else {
    message.warning('自动复制失败，请长按或选中密码手动复制');
  }
};

interface ModalState extends TemporaryPasswordModalOptions {
  open: boolean;
  password: string;
}

/**
 * 基于 Umi Max / Ant Design Pro 的 ModalForm + ProFormText 实现的临时密码表单弹窗 Hook
 * 与页面内“添加账号”“重置密码”等 ModalForm 保持完全一致的组件体系与响应式布局
 */
export const useTemporaryPasswordModal = () => {
  const [state, setState] = useState<ModalState>({
    open: false,
    title: '账号临时密码提示',
    account: '',
    userName: '',
    password: '',
  });

  const showTemporaryPasswordModal = useCallback((options: TemporaryPasswordModalOptions) => {
    const password = (
      options.temporaryPassword || extractTemporaryPassword(options.rawMessage)
    ).trim();
    setState({
      open: true,
      title: options.title || '账号临时密码提示',
      account: options.account,
      userName: options.userName,
      password: password || options.rawMessage || '',
    });
  }, []);

  const temporaryPasswordModalNode = (
    <ModalForm
      title={state.title}
      open={state.open}
      onOpenChange={(open) => {
        if (!open) {
          setState((prev) => ({...prev, open: false}));
        }
      }}
      modalProps={{
        destroyOnClose: true,
        maskClosable: false,
        keyboard: false,
      }}
      submitter={{
        searchConfig: {
          submitText: '复制密码并关闭',
          resetText: '关闭',
        },
      }}
      onFinish={async () => {
        if (state.password) {
          await copyToClipboard(state.password, '临时密码已复制到剪贴板');
        }
        return true;
      }}
    >
      <Alert
        type="warning"
        showIcon
        message="临时密码仅本次显示一次，服务端不保存明文，请立即复制并妥善保存"
        style={{marginBottom: 16}}
      />
      {state.account && (
        <ProFormText
          width="md"
          name="account"
          label="账号"
          initialValue={state.account}
          fieldProps={{
            readOnly: true,
          }}
        />
      )}
      {state.userName && (
        <ProFormText
          width="md"
          name="userName"
          label="姓名"
          initialValue={state.userName}
          fieldProps={{
            readOnly: true,
          }}
        />
      )}
      <ProFormText
        width="md"
        name="temporaryPassword"
        label="临时密码"
        initialValue={state.password}
        fieldProps={{
          readOnly: true,
          onFocus: (e) => e.target.select(),
          addonAfter: state.password ? (
            <Typography.Link
              onClick={() => copyToClipboard(state.password, '临时密码已复制到剪贴板')}
            >
              <CopyOutlined /> 复制
            </Typography.Link>
          ) : undefined,
        }}
      />
    </ModalForm>
  );

  return {
    showTemporaryPasswordModal,
    temporaryPasswordModalNode,
  };
};
