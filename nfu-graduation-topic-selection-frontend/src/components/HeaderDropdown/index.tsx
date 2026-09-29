import { Dropdown } from 'antd';
import type { DropDownProps } from 'antd/es/dropdown';
import React from 'react';
import { createStyles } from 'antd-style';
import classNames from 'classnames';

/**
 * 下拉弹出层响应式样式：
 * - 将宽度约束作用在内部 .ant-dropdown-menu 上，避免干扰外层 .ant-dropdown 展开/收缩过渡动画；
 * - 卡片内仅保留退出登录（及可选角色切换），桌面端与移动端宽度均自适应内容紧凑展示。
 */
const useStyles = createStyles(({ token }) => {
  return {
    dropdown: {
      '& .ant-dropdown-menu': {
        width: 'max-content',
        minWidth: '116px',
        maxWidth: '180px',
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

export type HeaderDropdownProps = {
  overlayClassName?: string;
  placement?: 'bottomLeft' | 'bottomRight' | 'topLeft' | 'topCenter' | 'topRight' | 'bottomCenter';
} & Omit<DropDownProps, 'overlay'>;

/**
 * 顶部与侧边栏用户菜单通用下拉容器组件：
 * 仅使用 click 触发，避免 hover 模式下鼠标在触发区域与下拉卡片之间移动时
 * 因 mouseLeave 事件导致下拉卡片意外关闭，无法稳定点击退出登录等菜单项。
 */
const HeaderDropdown: React.FC<HeaderDropdownProps> = ({
  overlayClassName: cls,
  trigger = ['click'],
  ...restProps
}) => {
  const { styles } = useStyles();
  return (
    <Dropdown
      trigger={trigger}
      overlayClassName={classNames(styles.dropdown, cls)}
      {...restProps}
    />
  );
};

export default HeaderDropdown;

