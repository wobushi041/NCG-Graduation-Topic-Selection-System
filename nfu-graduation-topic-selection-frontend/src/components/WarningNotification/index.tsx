import {ExclamationCircleFilled} from '@ant-design/icons';
import {Image, Modal} from 'antd';
import React from 'react';

/**
 * 登录成功后展示居中的系统提醒弹窗
 */
const showWarningNotification = () => {
  Modal.warning({
    icon: null,
    content: (
      <div style={{width: '100%', maxWidth: 496, margin: '0 auto'}}>
        <div style={{display: 'flex', alignItems: 'center', gap: 16, marginBottom: 16}}>
          <ExclamationCircleFilled style={{color: '#faad14', fontSize: 32}}/>
          <span style={{fontSize: 20, fontWeight: 600}}>本系统提醒</span>
        </div>
        <div>
          严禁爬虫抓取、恶意刷取、压力攻击等网络非法行为，本站由腾讯云承当部分服务保护。违者一经发现，
          <span style={{color: 'blue'}}>立刻封禁帐号</span>，上报学院处理，
          <span style={{color: 'red'}}>严重事故将追究相应的法律责任</span>，还请自重自爱！
          <a href="https://www.gov.cn/xinwen/2016-11/07/content_5129723.htm">
            详情见《中华人民共和国网络安全法》相关法条。
          </a>
          <Image src="/gdsgat.png" style={{width: '100%', borderRadius: 8}}/>
        </div>
      </div>
    ),
    centered: true,
    closable: true,
    width: 576,
    okText: '我已知悉',
  });
};

export default showWarningNotification;
