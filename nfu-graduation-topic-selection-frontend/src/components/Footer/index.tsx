/**
 * 全局底部组件：沉底展示广州南方学院毕设选题管理系统版权声明（不含顶部分隔线与外链图标）。
 *
 * @author <a href="https://github.com/limou3434">limou3434</a>
 */

import { DefaultFooter } from '@ant-design/pro-components';
import React from 'react';

/**
 * 渲染沉底展示的站点版权页脚组件
 */
const Footer: React.FC = () => {
  const currentYear = new Date().getFullYear();
  return (
    <DefaultFooter
      style={{
        background: 'none',
        marginTop: 'auto',
        marginBottom: 0,
        padding: '12px 0',
      }}
      links={[]}
      copyright={`${currentYear} 广州南方学院毕设选题管理系统`}
    />
  );
};

export default Footer;
