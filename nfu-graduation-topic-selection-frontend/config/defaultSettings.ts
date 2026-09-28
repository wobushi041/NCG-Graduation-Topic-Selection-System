import { ProLayoutProps } from '@ant-design/pro-components';

/**
 * 全局布局与主题默认设置（广州南方学院毕业选题管理系统标题、校徽 Logo 与加宽侧边栏宽度）
 */
const Settings: ProLayoutProps & {
  pwa?: boolean;
  logo?: string;
} = {
  navTheme: 'light',
  // 拂晓蓝
  colorPrimary: '#1890ff',
  layout: 'mix',
  contentWidth: 'Fluid',
  fixedHeader: false,
  fixSiderbar: true,
  siderWidth: 280,
  colorWeak: false,
  title: '广州南方学院毕业选题管理系统',
  pwa: true,
  logo: '/nfu-logo-512.png',
  iconfontUrl: '',
  token: {
    // 参见ts声明，demo 见文档，通过token 修改样式
    //https://procomponents.ant.design/components/layout#%E9%80%9A%E8%BF%87-token-%E4%BF%AE%E6%94%B9%E6%A0%B7%E5%BC%8F
  },
};

export default Settings;
