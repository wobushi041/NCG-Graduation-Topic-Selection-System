import {Footer} from '@/components';
import {InfoCircleOutlined, LockOutlined, UserOutlined} from '@ant-design/icons';
import {LoginForm, ProFormText} from '@ant-design/pro-components';
import {useIntl} from '@ant-design/pro-provider';
import {Helmet, history, Link} from '@umijs/max';
import {message, Tabs, Tooltip} from 'antd';
import {createStyles} from 'antd-style';
import React, {useRef, useState} from 'react';
import Settings from '../../../../config/defaultSettings';
import {
  changePassword,
  resetPasswordByCode,
  sendEmailVerificationCode,
  sendPasswordResetCode,
  verifyEmailCode,
} from '@/services/topic-selection/authController';

const useStyles = createStyles(({token}) => ({
  container: {
    display: 'flex',
    flexDirection: 'column',
    height: '100vh',
    overflow: 'auto',
    backgroundImage:
      "url('https://mdn.alipayobjects.com/yuyan_qk0oxh/afts/img/V-_oS6r-i7wAAAAAAAAAAAAAFl94AQBr')",
    backgroundSize: '100% 100%',
    '& .ant-pro-form-login-header': {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      height: '42px',
      lineHeight: 1,
      gap: '10px',
      whiteSpace: 'nowrap',
    },
    '& .ant-pro-form-login-logo': {
      width: '42px',
      height: '42px',
      margin: 0,
      marginRight: 0,
      display: 'inline-flex',
      alignItems: 'center',
      justifyContent: 'center',
      flexShrink: 0,
      verticalAlign: 'middle',
      '& > img': {
        width: '42px',
        height: '42px',
        display: 'block',
        margin: 0,
        objectFit: 'contain',
      },
    },
    '& .ant-pro-form-login-title': {
      position: 'static',
      top: 0,
      insetBlockStart: 0,
      margin: 0,
      display: 'inline-flex',
      alignItems: 'center',
      height: '42px',
      fontSize: '24px',
      lineHeight: 1,
      whiteSpace: 'nowrap',
    },
    [`@media screen and (max-width: ${token.screenSM}px)`]: {
      '& .ant-pro-form-login-header': {
        height: '34px',
        gap: '8px',
      },
      '& .ant-pro-form-login-logo': {
        width: '34px',
        height: '34px',
        '& > img': {
          width: '34px',
          height: '34px',
        },
      },
      '& .ant-pro-form-login-title': {
        height: '34px',
        fontSize: '19px',
        lineHeight: 1,
      },
      '& .ant-pro-form-login-desc': {
        fontSize: '13px',
        marginTop: '8px',
      },
    },
  },
}));

/**
 * 修改密码与邮箱重置码找回页面组件：
 * - 默认模式（!useTempPassword）：使用原密码或管理员分配的16位初始临时密码修改密码，并支持验证及首次绑定邮箱；
 * - 邮箱重置码模式（useTempPassword）：向已绑定邮箱发送12位重置码并凭重置码重置密码。
 */
const Register: React.FC = () => {
  const {styles} = useStyles();
  useIntl();

  const [type, setType] = useState<string>('account');
  const formRef = useRef<any>();
  const [countdown, setCountdown] = useState<number>(0);
  const [useTempPassword, setUseTempPassword] = useState<boolean>(false);
  const [emailCountdown, setEmailCountdown] = useState<number>(0); // 邮箱验证码倒计时
  const [emailForCaptcha, setEmailForCaptcha] = useState<string>(''); // 用于验证码的邮箱
  const [showCaptchaInput, setShowCaptchaInput] = useState<boolean>(false); // 是否显示验证码输入框
  const [emailProofToken, setEmailProofToken] = useState<string>('');

  /**
   * 向账号已绑定的邮箱发送 12 位密码重置码，并切换至邮箱重置码表单模式
   */
  const handleSendCode = async () => {
    const userAccount = formRef.current?.getFieldValue('userAccount');
    if (!userAccount) {
      message.error('请先输入账号');
      return;
    }

    try {
      const res = await sendPasswordResetCode(userAccount);
      if (res.code === 0) {
        message.success(res.data || '若账号已绑定邮箱，12位重置码已发送至绑定邮箱');
        setCountdown(60);
        setUseTempPassword(true); // 切换到邮箱重置码模式
        setShowCaptchaInput(false);
        setEmailProofToken('');
        formRef.current?.setFieldValue('email', undefined);
        formRef.current?.setFieldValue('emailCaptcha', undefined);
        const timer = setInterval(() => {
          setCountdown(prev => {
            if (prev <= 1) {
              clearInterval(timer);
              return 0;
            }
            return prev - 1;
          });
        }, 1000);
      } else {
        message.error(res.message);
      }
    } catch {
      message.error('发送失败，请稍后重试');
    }
  };

  // 发送邮箱验证码
  const handleSendCaptcha = async () => {
    const email = formRef.current?.getFieldValue('email');
    if (!email) {
      message.error('请先输入邮箱');
      return;
    }

    // 验证邮箱格式
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      message.error('请输入正确的邮箱格式');
      return;
    }

    // 检查是否是支持的邮箱类型
    const supportedEmails = ['qq.com', 'gmail.com', 'nfu.edu.cn'];
    const emailDomain = email.split('@')[1];
    if (!supportedEmails.includes(emailDomain)) {
      message.error('本系统仅支持 QQ 邮箱、Gmail 邮箱、校内邮箱');
      return;
    }

    try {
      const res = await sendEmailVerificationCode(email);
      if (res.code === 0) {
        message.success('验证码已发送，请查收邮件');
        setEmailForCaptcha(email);
        setShowCaptchaInput(true);
        setEmailProofToken('');
        setEmailCountdown(60);
        const timer = setInterval(() => {
          setEmailCountdown(prev => {
            if (prev <= 1) {
              clearInterval(timer);
              return 0;
            }
            return prev - 1;
          });
        }, 1000);
      } else {
        message.error(res.message);
      }
    } catch {
      message.error('验证码发送失败，请稍后重试');
    }
  };

  // 验证邮箱验证码
  const handleCheckCaptcha = async () => {
    const email = formRef.current?.getFieldValue('email');
    const captcha = formRef.current?.getFieldValue('emailCaptcha');
    if (!captcha) {
      message.error('请输入验证码');
      return '';
    }
    if (!emailForCaptcha || email !== emailForCaptcha) {
      message.error('邮箱已更改，请重新获取验证码');
      return '';
    }

    try {
      const res = await verifyEmailCode(email, captcha);
      if (res.code === 0 && res.data?.proofToken) {
        setEmailProofToken(res.data.proofToken);
        message.success('邮箱验证成功');
        return res.data.proofToken;
      } else {
        message.error(res.message);
        return '';
      }
    } catch {
      message.error('验证码校验失败，请稍后重试');
      return '';
    }
  };

  // 提交修改密码
  const handleSubmit = async (values: any) => {
    const {updatePassword, updatePassword2, userPassword, tempPasswordInput, userAccount, email, emailCaptcha} = values;

    if (updatePassword !== updatePassword2) {
      message.error('两次输入的密码不一致');
      return;
    }

    // 密码强度检查
    if (updatePassword.length < 8) {
      message.error('新密码长度不能少于8位');
      return;
    }

    let verifiedProofToken = emailProofToken;
    // 邮箱绑定只属于“当前密码修改”流程，验证码换取一次性 proofToken。
    if (email && !useTempPassword) {
      // 验证邮箱格式
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(email)) {
        message.error('请输入正确的邮箱格式');
        return;
      }

      // 检查是否是支持的邮箱类型
      const supportedEmails = ['qq.com', 'gmail.com', 'nfu.edu.cn'];
      const emailDomain = email.split('@')[1];
      if (!supportedEmails.includes(emailDomain)) {
        message.error('本系统仅支持 QQ 邮箱、Gmail 邮箱、校内邮箱');
        return;
      }

      // 如果用户输入了验证码，则需要验证验证码
      if (emailCaptcha) {
        // 验证验证码
        verifiedProofToken = await handleCheckCaptcha();
        if (!verifiedProofToken) {
          return; // 验证失败，不继续提交
        }
      } else {
        // 如果用户输入了邮箱但没有输入验证码，需要先获取验证码
        message.error('请先获取并输入邮箱验证码');
        return;
      }
    }

    try {
      let res;
      if (useTempPassword) {
        if (!tempPasswordInput) {
          message.error('请输入12位邮箱重置码');
          return;
        }
        res = await resetPasswordByCode({
          account: userAccount,
          resetCode: tempPasswordInput.trim(),
          newPassword: updatePassword,
        });
      } else {
        if (!userPassword) {
          message.error('请输入原密码或初始临时密码');
          return;
        }
        if (email && !verifiedProofToken) {
          message.error('请先完成邮箱验证码校验');
          return;
        }
        res = await changePassword({
          account: userAccount,
          currentPassword: userPassword,
          newPassword: updatePassword,
          email: email || undefined,
          emailProofToken: verifiedProofToken || undefined,
        });
      }
      if (res.code === 0) {
        setEmailProofToken('');
        message.success('密码修改成功');
        history.push('/user/login');
      } else {
        message.error(res.message);
      }
    } catch (error) {
      message.error('修改失败，请稍后重试');
    }
  };

  return (
    <div className={styles.container}>
      <Helmet>
        <title>{'修改密码'}- {Settings.title}</title>
      </Helmet>
      <div style={{flex: 1, padding: '32px 0'}}>
        <LoginForm
          formRef={formRef}
          contentStyle={{minWidth: 280, maxWidth: '75vw'}}
          submitter={{searchConfig: {submitText: '修改密码'}}}
          logo={<img alt="logo" src="/nfu-logo-512.png"/>}
          title="广州南方学院毕设选题管理系统"
          subTitle="NCG Graduation Topic Selection System"
          onFinish={async values => await handleSubmit(values)}
        >
          <Tabs
            activeKey={type}
            onChange={setType}
            centered
            items={[{key: 'account', label: '修改密码'}]}
          />
          {type === 'account' && (
            <>
              <ProFormText
                name="userAccount"
                label={
                  <span>
                    账号
                  </span>
                }
                fieldProps={{size: 'large', prefix: <UserOutlined/>}}
                placeholder="请输入账户"
                rules={[{required: true, message: '账号必填！'}]}
              />

              {!useTempPassword && (
                <ProFormText.Password
                  name="userPassword"
                  label={
                    <span>
                      原密码 / 初始临时密码
                    </span>
                  }
                  fieldProps={{size: 'large', prefix: <LockOutlined/>}}
                  placeholder="请输入原密码或管理员分配的16位初始临时密码"
                  rules={[{required: true, message: '请输入原密码或初始临时密码'}]}
                />
              )}

              {useTempPassword && (
                <ProFormText.Password
                  name="tempPasswordInput"
                  label={
                    <span>
                      邮箱重置码（12位）
                    </span>
                  }
                  fieldProps={{size: 'large', prefix: <LockOutlined/>}}
                  placeholder="请输入绑定邮箱收到的12位重置码"
                  rules={[
                    {required: true, message: '请输入邮箱重置码'},
                    {len: 12, message: '邮箱重置码长度应为12位（管理员分配的16位初始密码请切换回“原密码/初始临时密码”填写）'},
                  ]}
                />
              )}

              <ProFormText.Password
                name="updatePassword"
                label={
                  <span>
                    新密码
                  </span>
                }
                fieldProps={{size: 'large', prefix: <LockOutlined/>}}
                placeholder="请输入新密码"
                rules={[{required: true, min: 8, message: '新密码不少于8位'}]}
              />
              <ProFormText.Password
                name="updatePassword2"
                label={
                  <span>
                    确认新密码
                  </span>
                }
                fieldProps={{size: 'large', prefix: <LockOutlined/>}}
                placeholder="请再次输入新密码"
                rules={[{required: true, min: 8, message: '新密码不少于8位'}]}
              />

              {!useTempPassword && (
                <ProFormText
                  name="email"
                  label={<span>绑定邮箱（选填）</span>}
                  fieldProps={{
                    size: 'large',
                    prefix: <UserOutlined/>,
                    suffix: (
                      <Tooltip
                        title="本系统支持 QQ 邮箱、Gmail 邮箱和校内邮箱（nfu.edu.cn），绑定后可在忘记密码时接收 12 位邮箱重置码；若未绑定邮箱且忘记密码，请联系管理员重置初始临时密码。"
                        placement="right"
                      >
                        <InfoCircleOutlined style={{color: 'rgba(0,0,0,.45)'}}/>
                      </Tooltip>
                    ),
                    onChange: (event) => {
                      const email = event.target.value;
                      setShowCaptchaInput(Boolean(email));
                      setEmailForCaptcha('');
                      setEmailProofToken('');
                      setEmailCountdown(0);
                      formRef.current?.setFieldValue('emailCaptcha', undefined);
                    },
                  }}
                  placeholder="请输入邮箱（选填，首次登录建议绑定）"
                  rules={[
                    {required: false, message: '邮箱选填！'},
                    {type: 'email', message: '请输入正确的邮箱格式'}
                  ]}
                />
              )}

              {/* 邮箱验证码区域 */}
              {!useTempPassword && showCaptchaInput && (
                <div style={{
                  backgroundColor: '#f0f8ff',
                  padding: '16px',
                  borderRadius: '4px',
                  marginBottom: '16px',
                  border: '1px solid #d9d9d9'
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', marginBottom: '12px' }}>
                    <span style={{ fontWeight: 500, marginRight: '8px' }}>邮箱验证</span>
                    <Tooltip title="为确保邮箱有效性，需要验证您对该邮箱的所有权">
                      <InfoCircleOutlined style={{ color: 'rgba(0,0,0,.45)' }} />
                    </Tooltip>
                  </div>

                  <div style={{ display: 'flex', gap: '8px', alignItems: 'flex-start' }}>
                    <ProFormText
                      name="emailCaptcha"
                      fieldProps={{
                        size: 'large',
                        placeholder: "请输入6位数字验证码",
                        autoComplete: "one-time-code",
                        style: { flex: 1 }
                      }}
                      rules={[{required: false}]}
                      noStyle
                    />
                    <a
                      style={{
                        whiteSpace: 'nowrap',
                        cursor: emailCountdown > 0 ? 'not-allowed' : 'pointer',
                        color: emailCountdown > 0 ? '#999' : '#1890ff',
                        height: '32px',
                        lineHeight: '32px'
                      }}
                      onClick={() => emailCountdown === 0 && handleSendCaptcha()}
                    >
                      {emailCountdown > 0 ? `重新获取(${emailCountdown}s)` : '获取验证码'}
                    </a>
                  </div>
                  <div style={{
                    fontSize: '12px',
                    color: '#666',
                    marginTop: '4px'
                  }}>
                    验证码将发送到 {formRef.current?.getFieldValue('email') || emailForCaptcha || '您输入的邮箱'}
                  </div>
                </div>
              )}

            </>
          )}

          <div style={{marginBottom: 24}}>
            <div style={{marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '8px'}}>
              <div style={{display: 'flex', gap: '12px', flexWrap: 'wrap'}}>
                <a
                  style={{
                    cursor: countdown > 0 ? 'not-allowed' : 'pointer',
                    color: countdown > 0 ? '#999' : '#1890ff'
                  }}
                  onClick={() => countdown === 0 && handleSendCode()}
                >
                  {countdown > 0 ? `重新获取邮箱重置码(${countdown}s)` : '已绑定邮箱？获取邮箱重置码'}
                </a>
                {useTempPassword && (
                  <a
                    style={{cursor: 'pointer', color: '#1890ff'}}
                    onClick={() => {
                      setUseTempPassword(false);
                      formRef.current?.setFieldValue('tempPasswordInput', undefined);
                    }}
                  >
                    返回原密码/初始密码修改
                  </a>
                )}
              </div>
              <Link to="/user/login" style={{float: 'right'}}>
                返回登录页面
              </Link>
            </div>
          </div>
        </LoginForm>
      </div>
      <Footer/>
    </div>
  );
};

export default Register;
