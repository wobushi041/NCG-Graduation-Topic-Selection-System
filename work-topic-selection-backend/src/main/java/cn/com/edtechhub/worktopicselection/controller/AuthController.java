package cn.com.edtechhub.worktopicselection.controller;

import cn.com.edtechhub.worktopicselection.annotation.SentinelRateLimit;
import cn.com.edtechhub.worktopicselection.annotation.ValidateRequest;
import cn.com.edtechhub.worktopicselection.exception.CodeBindMessageEnums;
import cn.com.edtechhub.worktopicselection.model.dto.auth.AdminResetPasswordRequest;
import cn.com.edtechhub.worktopicselection.model.dto.auth.ChangePasswordRequest;
import cn.com.edtechhub.worktopicselection.model.dto.auth.LoginRequest;
import cn.com.edtechhub.worktopicselection.model.dto.auth.ResetPasswordByCodeRequest;
import cn.com.edtechhub.worktopicselection.model.dto.auth.RoleSwitchRequest;
import cn.com.edtechhub.worktopicselection.model.dto.auth.SendEmailCodeRequest;
import cn.com.edtechhub.worktopicselection.model.dto.auth.SendResetCodeRequest;
import cn.com.edtechhub.worktopicselection.model.dto.auth.VerifyEmailCodeRequest;
import cn.com.edtechhub.worktopicselection.model.vo.AdminResetPasswordVO;
import cn.com.edtechhub.worktopicselection.model.vo.EmailVerificationVO;
import cn.com.edtechhub.worktopicselection.model.vo.LoginUserVO;
import cn.com.edtechhub.worktopicselection.model.vo.RoleSwitchAvailabilityVO;
import cn.com.edtechhub.worktopicselection.response.BaseResponse;
import cn.com.edtechhub.worktopicselection.response.TheResult;
import cn.com.edtechhub.worktopicselection.service.AuthenticationService;
import cn.com.edtechhub.worktopicselection.service.PasswordService;
import cn.com.edtechhub.worktopicselection.service.VerificationCodeService;
import cn.com.edtechhub.worktopicselection.utils.DeviceUtils;
import cn.com.edtechhub.worktopicselection.utils.IpUtils;
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
 * 认证、密码和验证码 HTTP 入口。
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final PasswordService passwordService;
    private final VerificationCodeService verificationCodeService;

    public AuthController(AuthenticationService authenticationService, PasswordService passwordService,
                          VerificationCodeService verificationCodeService) {
        this.authenticationService = authenticationService;
        this.passwordService = passwordService;
        this.verificationCodeService = verificationCodeService;
    }

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

    @SaCheckLogin
    @SentinelRateLimit(resource = "auth.logout")
    @PostMapping("/logout")
    public BaseResponse<Boolean> logout() {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, authenticationService.logout());
    }

    @SaCheckLogin
    @SaCheckRole(value = {"teacher", "dept"}, mode = SaMode.OR)
    @ValidateRequest
    @SentinelRateLimit(resource = "auth.role-switch")
    @PostMapping("/role-switch")
    public BaseResponse<LoginUserVO> switchRole(@RequestBody RoleSwitchRequest request,
                                                HttpServletRequest servletRequest) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS,
                authenticationService.switchRole(request, DeviceUtils.getRequestDevice(servletRequest)));
    }

    @SaCheckLogin
    @SentinelRateLimit(resource = "auth.role-switch-availability")
    @GetMapping("/role-switch/availability")
    public BaseResponse<RoleSwitchAvailabilityVO> roleSwitchAvailability() {
        return TheResult.success(CodeBindMessageEnums.SUCCESS,
                authenticationService.getRoleSwitchAvailability());
    }

    @SaCheckLogin
    @SaCheckRole("admin")
    @ValidateRequest
    @SentinelRateLimit(resource = "auth.password-admin-reset")
    @PostMapping("/password/admin-reset")
    public BaseResponse<AdminResetPasswordVO> adminReset(@RequestBody AdminResetPasswordRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, passwordService.adminReset(request));
    }

    @SaIgnore
    @ValidateRequest
    @SentinelRateLimit(resource = "auth.password-change")
    @PostMapping("/password/change")
    public BaseResponse<Long> changePassword(@RequestBody ChangePasswordRequest request,
                                             HttpServletRequest servletRequest) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS,
                passwordService.changePassword(request, IpUtils.getIpAddress(servletRequest)));
    }

    @SaIgnore
    @ValidateRequest
    @SentinelRateLimit(resource = "auth.password-reset")
    @PostMapping("/password/reset")
    public BaseResponse<Long> resetPassword(@RequestBody ResetPasswordByCodeRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, passwordService.resetPassword(request));
    }

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

    @SaIgnore
    @ValidateRequest
    @SentinelRateLimit(resource = "auth.email-code-send")
    @PostMapping("/email-verification/code/send")
    public BaseResponse<String> sendEmailCode(@RequestBody SendEmailCodeRequest request,
                                              HttpServletRequest servletRequest) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS,
                verificationCodeService.sendEmailCode(request.getEmail(), IpUtils.getIpAddress(servletRequest)));
    }

    @SaIgnore
    @ValidateRequest
    @SentinelRateLimit(resource = "auth.email-code-verify")
    @PostMapping("/email-verification/code/verify")
    public BaseResponse<EmailVerificationVO> verifyEmailCode(@RequestBody VerifyEmailCodeRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS,
                verificationCodeService.verifyEmailCode(request.getEmail(), request.getCode()));
    }
}
