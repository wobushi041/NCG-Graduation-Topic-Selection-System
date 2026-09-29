import {LogoutOutlined, SettingOutlined, SwapOutlined, UserOutlined} from '@ant-design/icons';
import {history, useModel} from '@umijs/max';
import {message, Modal, Spin} from 'antd';
import {createStyles} from 'antd-style';
import type {MenuInfo} from 'rc-menu/lib/interface';
import React, {useCallback, useEffect, useState} from 'react';
import {flushSync} from 'react-dom';
import HeaderDropdown from '../HeaderDropdown';
import {USER_ROLE_ENUM, USER_ROLE_MAP} from '@/constants/user';
import {getRoleSwitchAvailability, logout, switchRole} from '@/services/topic-selection/authController';

export type GlobalHeaderRightProps = {
  menu?: boolean;
  children?: React.ReactNode;
};

/**
 * 渲染当前登录用户的“角色 - 姓名”文本标签，并与左侧 18x18 身份图标保持像素级垂直居中对齐
 */
export const AvatarName = () => {
  const {initialState} = useModel('@@initialState');
  const {currentUser} = initialState || {};
  const displayText = currentUser
    ? `${USER_ROLE_MAP[currentUser.userRole as 0 | 1 | 2 | 3]} - ${currentUser.userName}`
    : '';

  return (
    <span
      className="nfu-sider-avatar-name"
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        maxWidth: 130,
        height: 18,
        lineHeight: '18px',
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
      }}
    >
      {displayText}
    </span>
  );
};

const useStyles = createStyles(({token}) => {
  return {
    action: {
      display: 'flex',
      height: '48px',
      marginLeft: 'auto',
      overflow: 'hidden',
      alignItems: 'center',
      padding: '0 8px',
      cursor: 'pointer',
      borderRadius: token.borderRadius,
      '&:hover': {
        backgroundColor: token.colorBgTextHover,
      },
    },
    /**
     * 退出登录下拉卡片宽度收缩及危险操作样式：
     * - 外部触发区域已横向展示「角色 - 姓名」，卡片内部仅保留退出登录（及可选身份切换），宽度自适应内容；
     * - 退出登录菜单项的图标与文字统一使用红色 (#ff4d4f)，悬浮时使用淡红底色。
     */
    avatarDropdownOverlay: {
      '& .ant-dropdown-menu': {
        width: 'max-content',
        minWidth: '116px',
        maxWidth: '180px',
      },
      '& .ant-dropdown-menu-item-danger': {
        color: '#ff4d4f !important',
        '& .ant-dropdown-menu-item-icon': {
          color: '#ff4d4f !important',
        },
        '&:hover': {
          color: '#ff4d4f !important',
          backgroundColor: 'rgba(255, 77, 79, 0.08) !important',
        },
      },
      [`@media screen and (max-width: ${token.screenMD}px)`]: {
        '& .ant-dropdown-menu': {
          width: 'max-content',
          minWidth: '112px',
          maxWidth: '168px',
        },
      },
      [`@media screen and (max-width: ${token.screenXS}px)`]: {
        '& .ant-dropdown-menu': {
          width: 'max-content',
          minWidth: '108px',
          maxWidth: '160px',
        },
      },
    },
  };
});

/**
 * 用户头像与身份下拉菜单组件：
 * 展示当前登录用户的姓名、角色、学院/专业信息，以及角色切换和退出登录操作，
 * 并针对桌面端与移动端分别收敛下拉卡片宽度。
 */
export const AvatarDropdown: React.FC<GlobalHeaderRightProps> = ({menu, children}) => {
  /**
   * 退出登录，并且将当前的 url 保存
   */
  const loginOut = async () => {
    const {search, pathname} = window.location;
    const urlParams = new URL(window.location.href).searchParams;
    /** 此方法会跳转到 redirect 参数所在的位置 */
    const redirect = urlParams.get('redirect');
    // Note: There may be security issues, please note
    if (window.location.pathname !== '/user/login' && !redirect) {
      history.replace({
        pathname: '/user/login',
        search: new URLSearchParams({redirect: pathname + search}).toString(),
      });
    }
  };
  const {styles} = useStyles();

  const {initialState, setInitialState} = useModel('@@initialState');
  const currentUser = initialState?.currentUser;

  // 仅当当前账号确实存在「已配对的另一角色账号」时才展示切换身份入口。
  // 此前的判断只看角色, 导致全体教师都能看到入口, 但点下去必然报错。
  const [canToggleRole, setCanToggleRole] = useState(false);
  useEffect(() => {
    const role = currentUser?.userRole;
    const switchableRole = role === USER_ROLE_ENUM.TEACHER || role === USER_ROLE_ENUM.DIRECTOR;
    if (!switchableRole) {
      setCanToggleRole(false);
      return;
    }
    let cancelled = false;
    getRoleSwitchAvailability()
      .then((res) => {
        if (!cancelled) {
          setCanToggleRole(res.code === 0 && res.data?.available === true);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setCanToggleRole(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [currentUser?.userRole]);

  const onMenuClick = useCallback(
    async (event: MenuInfo) => {
      const {key} = event;
      if (key === 'logout') {
        try {
          const res = await logout();
          if (res.code === 0) {
            message.success(res.message);
          } else {
            message.error(res.message);
            return;
          }
        } catch (error: any) {
          message.error(error.message);
          return;
        }

        flushSync(() => {
          setInitialState((s: any) => ({...s, currentUser: undefined}));
        });
        loginOut();
        // 强制刷新页面确保状态完全清除
        window.location.reload();
        return;
      }

      // 处理身份切换
      if (key === 'switch-role') {
        if (!currentUser) {
          return;
        }

        // 显示切换身份的确认对话框
        Modal.confirm({
          title: '切换身份',
          content: '确定要切换身份吗？',
          okText: '确认',
          cancelText: '取消',
          onOk: async () => {
            try {
              // 确定要切换到的角色
              let targetRole: 'teacher' | 'college';
              if (currentUser.userRole === USER_ROLE_ENUM.TEACHER) {
                targetRole = 'college';
              } else if (currentUser.userRole === USER_ROLE_ENUM.DIRECTOR) {
                targetRole = 'teacher';
              } else {
                return;
              }

              // 调用切换身份的接口
              const res = await switchRole(targetRole);

              // 检查返回值
              if (res.code === 0 && res.data) {
                // 更新初始状态
                flushSync(() => {
                  setInitialState((s: any) => ({...s, currentUser: res.data}));
                });

                message.success(res.message);
                // 切换身份后刷新页面并访问首页
                window.location.href = '/home';
              } else {
                message.error(res.message);
              }
            } catch (error: any) {
              message.error(error.message);
            }
          },
        });
        return;
      }

      history.push(`/account/${key}`);
    },
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    [setInitialState, currentUser],
  );

  const loading = (
    <span className={styles.action}>
      <Spin
        size="small"
        style={{
          marginLeft: 8,
          marginRight: 8,
        }}
      />
    </span>
  );

  if (!initialState) {
    return loading;
  }

  if (!currentUser || !currentUser.userName) {
    return loading;
  }

  // 只有选题负责人和教师角色, 且确实存在已配对的另一角色账号时才显示切换身份按钮
  const showSwitchRole = canToggleRole;

  const menuItems = [
    ...(menu
      ? [
        {
          key: 'center',
          icon: <UserOutlined/>,
          label: '个人中心',
        },
        {
          key: 'settings',
          icon: <SettingOutlined/>,
          label: '个人设置',
        },
        {
          type: 'divider' as const,
        },
      ]
      : []),
    // 添加切换身份按钮
    ...(showSwitchRole ? [
      {
        key: 'switch-role',
        icon: <SwapOutlined />,
        label: `切换${currentUser.userRole === USER_ROLE_ENUM.TEACHER ? '选题负责人' : '教师'}`,
      },
      {
        type: 'divider' as const,
      },
    ] : []),
    {
      key: 'logout',
      danger: true,
      icon: <LogoutOutlined style={{color: '#ff4d4f'}} />,
      label: <span style={{color: '#ff4d4f'}}>退出登录</span>,
    },
  ];

  return (
    <HeaderDropdown
      overlayClassName={styles.avatarDropdownOverlay}
      menu={{
        selectedKeys: [],
        onClick: onMenuClick,
        items: menuItems,
      }}
      overlayStyle={{
        padding: '4px 0',
      }}
    >
      {children}
    </HeaderDropdown>
  );
};
