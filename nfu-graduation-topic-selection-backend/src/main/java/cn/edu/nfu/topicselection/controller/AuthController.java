package cn.edu.nfu.topicselection.controller;

import cn.edu.nfu.topicselection.annotation.SentinelRateLimit;
import cn.edu.nfu.topicselection.annotation.ValidateRequest;
import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import cn.edu.nfu.topicselection.model.request.auth.AdminResetPasswordRequest;
import cn.edu.nfu.topicselection.model.request.auth.ChangePasswordRequest;
import cn.edu.nfu.topicselection.model.request.auth.LoginRequest;
import cn.edu.nfu.topicselection.model.request.auth.ResetPasswordByCodeRequest;
import cn.edu.nfu.topicselection.model.request.auth.RoleSwitchRequest;
import cn.edu.nfu.topicselection.model.request.auth.SendEmailCodeRequest;
import cn.edu.nfu.topicselection.model.request.auth.SendResetCodeRequest;
import cn.edu.nfu.topicselection.model.request.auth.VerifyEmailCodeRequest;
import cn.edu.nfu.topicselection.model.vo.AdminResetPasswordVO;
import cn.edu.nfu.topicselection.model.vo.EmailVerificationVO;
import cn.edu.nfu.topicselection.model.vo.LoginUserVO;
import cn.edu.nfu.topicselection.model.vo.RoleSwitchAvailabilityVO;
import cn.edu.nfu.topicselection.response.BaseResponse;
import cn.edu.nfu.topicselection.response.TheResult;
import cn.edu.nfu.topicselection.service.AuthenticationService;
import cn.edu.nfu.topicselection.service.PasswordService;
import cn.edu.nfu.topicselection.service.VerificationCodeService;
import cn.edu.nfu.topicselection.utils.DeviceUtils;
import cn.edu.nfu.topicselection.utils.IpUtils;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.annotation.SaMode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 认证、密码和验证码 HTTP 入口
 *
 * @author wobushi041
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    /**
     * 注入认证服务依赖
     */
    private final AuthenticationService authenticationService;

    /**
     * 注入密码服务依赖
     */
    private final PasswordService passwordService;

    /**
     * 注入验证码服务依赖
     */
    private final VerificationCodeService verificationCodeService;

    /**
     * 初始化认证模块 HTTP 入口
     *
     * @param authenticationService   认证服务
     * @param passwordService         密码服务
     * @param verificationCodeService 验证码服务
     */
    public AuthController(AuthenticationService authenticationService, PasswordService passwordService,
                          VerificationCodeService verificationCodeService) {
        this.authenticationService = authenticationService;
        this.passwordService = passwordService;
        this.verificationCodeService = verificationCodeService;
    }

    /// 登录会话 ///

    /**
     * 登录账号并创建认证会话
     *
     * @param request        登录请求
     * @param servletRequest HTTP 请求
     * @return 登录用户信息响应
     */
    @SaIgnore
    @ValidateRequest
    @SentinelRateLimit(resource = "auth.login")
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> login(@RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        LoginUserVO result = authenticationService.login(
                request,
                IpUtils.getIpAddress(servletRequest),
                DeviceUtils.getRequestDevice(servletRequest)
        );
        return TheResult.success(CodeBindMessageEnums.SUCCESS, result);
    }

    /**
     * 退出当前认证会话
     *
     * @return 退出结果响应
     */
    @SaCheckLogin
    @SentinelRateLimit(resource = "auth.logout")
    @PostMapping("/logout")
    public BaseResponse<Boolean> logout() {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, authenticationService.logout());
    }

    /// 角色切换 ///

    /**
     * 在教师与选题负责人身份之间切换认证会话
     *
     * @param request        角色切换请求
     * @param servletRequest HTTP 请求
     * @return 切换后的登录用户信息响应
     */
    @SaCheckLogin
    @SaCheckRole(value = {"teacher", "topic_leader"}, mode = SaMode.OR)
    @ValidateRequest
    @SentinelRateLimit(resource = "auth.role-switch")
    @PostMapping("/role-switch")
    public BaseResponse<LoginUserVO> switchRole(@RequestBody RoleSwitchRequest request,
                                                HttpServletRequest servletRequest) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS,
                authenticationService.switchRole(request, DeviceUtils.getRequestDevice(servletRequest)));
    }

    /**
     * 查询当前账号是否可以切换角色
     *
     * @return 角色切换可用性响应
     */
    @SaCheckLogin
    @SentinelRateLimit(resource = "auth.role-switch-availability")
    @GetMapping("/role-switch/availability")
    public BaseResponse<RoleSwitchAvailabilityVO> roleSwitchAvailability() {
        return TheResult.success(CodeBindMessageEnums.SUCCESS,
                authenticationService.getRoleSwitchAvailability());
    }

    /// 密码管理 ///

    /**
     * 由管理员为指定账号重置临时密码
     *
     * @param request 管理员重置密码请求
     * @return 账号与临时密码响应
     */
    @SaCheckLogin
    @SaCheckRole("admin")
    @ValidateRequest
    @SentinelRateLimit(resource = "auth.password-admin-reset")
    @PostMapping("/password/admin-reset")
    public BaseResponse<AdminResetPasswordVO> adminReset(@RequestBody AdminResetPasswordRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, passwordService.adminReset(request));
    }

    /**
     * 使用当前密码修改账号密码
     *
     * @param request        修改密码请求
     * @param servletRequest HTTP 请求
     * @return 用户 id 响应
     */
    @SaIgnore
    @ValidateRequest
    @SentinelRateLimit(resource = "auth.password-change")
    @PostMapping("/password/change")
    public BaseResponse<Long> changePassword(@RequestBody ChangePasswordRequest request,
                                             HttpServletRequest servletRequest) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS,
                passwordService.changePassword(request, IpUtils.getIpAddress(servletRequest)));
    }

    /**
     * 使用一次性重置码修改账号密码
     *
     * @param request 重置密码请求
     * @return 用户 id 响应
     */
    @SaIgnore
    @ValidateRequest
    @SentinelRateLimit(resource = "auth.password-reset")
    @PostMapping("/password/reset")
    public BaseResponse<Long> resetPassword(@RequestBody ResetPasswordByCodeRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, passwordService.resetPassword(request));
    }

    /**
     * 向账号绑定邮箱发送密码重置码
     *
     * @param request        发送重置码请求
     * @param servletRequest HTTP 请求
     * @return 通用发送结果响应
     */
    @SaIgnore
    @ValidateRequest
    @SentinelRateLimit(resource = "auth.password-reset-code-send")
    @PostMapping("/password/reset-code/send")
    public BaseResponse<String> sendResetCode(@RequestBody SendResetCodeRequest request,
                                              HttpServletRequest servletRequest) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS,
                verificationCodeService.sendPasswordResetCode(
                        request.getAccount(), IpUtils.getIpAddress(servletRequest)));
    }

    /// 邮箱验证 ///

    /**
     * 向指定邮箱发送验证码
     *
     * @param request        发送邮箱验证码请求
     * @param servletRequest HTTP 请求
     * @return 发送结果响应
     */
    @SaIgnore
    @ValidateRequest
    @SentinelRateLimit(resource = "auth.email-code-send")
    @PostMapping("/email-verification/code/send")
    public BaseResponse<String> sendEmailCode(@RequestBody SendEmailCodeRequest request,
                                              HttpServletRequest servletRequest) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS,
                verificationCodeService.sendEmailCode(request.getEmail(), IpUtils.getIpAddress(servletRequest)));
    }

    /**
     * 校验邮箱验证码并签发一次性凭证
     *
     * @param request 邮箱验证码校验请求
     * @return 邮箱验证凭证响应
     */
    @SaIgnore
    @ValidateRequest
    @SentinelRateLimit(resource = "auth.email-code-verify")
    @PostMapping("/email-verification/code/verify")
    public BaseResponse<EmailVerificationVO> verifyEmailCode(@RequestBody VerifyEmailCodeRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS,
                verificationCodeService.verifyEmailCode(request.getEmail(), request.getCode()));
    }

}
