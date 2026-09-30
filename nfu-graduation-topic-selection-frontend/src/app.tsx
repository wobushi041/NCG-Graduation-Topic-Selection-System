import { AvatarDropdown, AvatarName, Footer } from '@/components';
import { getLoginUserUsingGet } from '@/services/topic-selection/userController';
import { LinkOutlined } from '@ant-design/icons';
import { SettingDrawer } from '@ant-design/pro-components';
import type { Settings as LayoutSettings } from '@ant-design/pro-components';
import type { RunTimeLayoutConfig } from '@umijs/max';
import { history, Link } from '@umijs/max';
import { Watermark } from 'antd';
import defaultSettings from '../config/defaultSettings';
import { errorConfig } from './requestErrorConfig';
import { WebSocketNotification } from '@/components/WebSocket';
import React from 'react';

/**
 * 复刻 address-card-outline 样式的 18x18 像素对齐矢量 SVG 身份卡片图标组件
 */
const AddressCardOutlineIcon: React.FC = () => (
  <svg
    viewBox="0 0 18 18"
    width="18"
    height="18"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    shapeRendering="geometricPrecision"
    style={{ display: 'block', flexShrink: 0, transition: 'stroke 0.2s ease, fill 0.2s ease' }}
  >
    <rect x="1.75" y="3.75" width="14.5" height="10.5" rx="1.75" stroke="currentColor" strokeWidth="1.5" />
    <circle cx="6.5" cy="7.25" r="1.75" fill="currentColor" />
    <path
      d="M4 11.75C4 10.35 5.1 9.5 6.5 9.5C7.9 9.5 9 10.35 9 11.75V12.5H4V11.75Z"
      fill="currentColor"
    />
    <rect x="10.25" y="6.5" width="4" height="1.5" rx="0.75" fill="currentColor" />
    <rect x="10.25" y="10" width="4" height="1.5" rx="0.75" fill="currentColor" />
  </svg>
);

const isLocalDevelopment = ['127.0.0.1', 'localhost'].includes(window.location.hostname);
const host = isLocalDevelopment
  ? `${window.location.protocol}//${window.location.hostname}:8000`
  : `${window.location.origin}/api`;

const isDev = false;

const loginPath = '/user/login';
const publicAuthPaths = new Set([loginPath, '/user/register']);

/**
 * 全局初始状态获取函数：注入 defaultSettings 布局配置并在非公开认证路由下自动拉取当前登录用户信息
 * @see https://umijs.org/zh-CN/plugins/plugin-initial-state
 */
export async function getInitialState(): Promise<{
  settings?: Partial<LayoutSettings>;
  currentUser?: API.LoginUserVO;
  collapsed?: boolean;
}> {
  const fetchUserInfo = async () => {
    try {
      const res = await getLoginUserUsingGet();
      return res.data;
    } catch (error) {
      history.push(loginPath);
    }
    return undefined;
  };
  // 如果不是登录页面，执行
  const { location } = history;
  if (!publicAuthPaths.has(location.pathname)) {
    const currentUser = await fetchUserInfo();
    return {
      currentUser,
      collapsed: false,
      settings: defaultSettings as Partial<LayoutSettings>,
    };
  }

  return {
    collapsed: false,
    settings: defaultSettings as Partial<LayoutSettings>,
  };
}

/**
 * 全局 ProLayout 运行时布局配置：
 * - 顶栏/侧栏左上角强制渲染广州南方学院校徽 (/nfu-logo-512.png)，展开时显示单行系统名称，收缩时仅居中展示校徽；
 * - 设置紧凑的左侧侧边栏宽度 (siderWidth: 248)，并保持菜单标题单行显示；
 * - 左下角在展开时展示像素级对齐的 AddressCardOutlineIcon + 角色姓名，在收缩时仅居中展示 AddressCardOutlineIcon 图标。
 * @see https://procomponents.ant.design/components/layout
 */
export const layout: RunTimeLayoutConfig = ({ initialState, setInitialState }) => {
  const isCollapsed = Boolean(initialState?.collapsed);

  return {
    logo: '/nfu-logo-512.png',
    title: '广州南方学院毕设选题管理系统',
    siderWidth: 248,
    collapsed: isCollapsed,
    onCollapse: (collapsed: boolean) => {
      setInitialState((preInitialState) => ({
        ...preInitialState,
        collapsed,
      }));
    },
    headerTitleRender: () => {
      return (
        <div
          className="nfu-sider-header-wrapper"
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            height: 32,
            gap: 8,
            whiteSpace: 'nowrap',
          }}
        >
          <img
            src="/nfu-logo-512.png"
            alt="logo"
            style={{
              width: 32,
              height: 32,
              display: 'block',
              margin: 0,
              objectFit: 'contain',
              flexShrink: 0,
            }}
          />
          <span
            className="nfu-sider-header-title"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              height: 32,
              fontWeight: 600,
              fontSize: 16,
              color: 'rgba(0, 0, 0, 0.88)',
              lineHeight: 1,
            }}
          >
            广州南方学院毕设选题管理系统
          </span>
        </div>
      );
    },
    avatarProps: {
      src: undefined,
      size: 'small',
      icon: <AddressCardOutlineIcon />,
      style: {
        backgroundColor: 'transparent',
        width: 18,
        height: 18,
        lineHeight: '18px',
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
      },
      title: <AvatarName />,
      render: () => {
        return (
          <AvatarDropdown>
            <span
              className="nfu-sider-avatar-wrapper"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 8,
                height: 28,
                padding: '0 6px',
                borderRadius: 6,
                cursor: 'pointer',
                lineHeight: '18px',
                whiteSpace: 'nowrap',
                transition: 'all 0.2s cubic-bezier(0.645, 0.045, 0.355, 1)',
              }}
            >
              <AddressCardOutlineIcon />
              <AvatarName />
            </span>
          </AvatarDropdown>
        );
      },
    },
    style: {
      minHeight: '100vh',
    },
    contentStyle: {
      flex: '1 0 auto',
      display: 'flex',
      flexDirection: 'column',
    },
    footerRender: () => <Footer />,
    onPageChange: () => {
      const { location } = history;
      // 如果没有登录，重定向到 login
      if (!initialState?.currentUser && !publicAuthPaths.has(location.pathname)) {
        history.push(loginPath);
      }
    },
    bgLayoutImgList: [
      {
        src: 'https://mdn.alipayobjects.com/yuyan_qk0oxh/afts/img/D2LWSqNny4sAAAAAAAAAAAAAFl94AQBr',
        left: 85,
        bottom: 100,
        height: '303px',
      },
      {
        src: 'https://mdn.alipayobjects.com/yuyan_qk0oxh/afts/img/C2TWRpJpiC0AAAAAAAAAAAAAFl94AQBr',
        bottom: -68,
        right: -45,
        height: '303px',
      },
      {
        src: 'https://mdn.alipayobjects.com/yuyan_qk0oxh/afts/img/F6vSTbj8KpYAAAAAAAAAAAAAFl94AQBr',
        bottom: 0,
        left: 0,
        width: '331px',
      },
    ],
    links: isDev
      ? [
          <Link key="openapi" to="/umi/plugin/openapi" target="_blank">
            <LinkOutlined />
            <span>OpenAPI 文档</span>
          </Link>,
        ]
      : [],
    menuHeaderRender: undefined,
    // 自定义 403 页面
    // unAccessible: <div>unAccessible</div>,
    // 增加一个 loading 的状态
    childrenRender: (children) => {
      // if (initialState?.loading) return <PageLoading />;

      return (
        <>
          {initialState?.currentUser?.userName && (
            <Watermark
              content={initialState.currentUser.userName}
              gap={[160, 160]}
              font={{
                color: 'rgba(0, 0, 0, 0.12)',
                fontSize: 16,
              }}
              style={{
                position: 'fixed',
                inset: 0,
                zIndex: 999,
                pointerEvents: 'none',
              }}
            />
          )}
          <WebSocketNotification />
          {children}
          {isDev && (
            <SettingDrawer
              disableUrlParams
              enableDarkTheme
              // @ts-ignore
              settings={initialState?.settings}
              onSettingChange={(settings) => {
                setInitialState((preInitialState) => ({
                  ...preInitialState,
                  settings,
                }));
              }}
            />
          )}
        </>
      );
    },
    // @ts-ignore
    ...initialState?.settings,
  };
};

/**
 * @name request 配置，可以配置错误处理
 * 它基于 axios 和 ahooks 的 useRequest 提供了一套统一的网络请求和错误处理方案。
 * @doc https://umijs.org/docs/max/request#配置
 */
export const request = {
  baseURL: host,
  withCredentials: true,
  ...errorConfig,
};
