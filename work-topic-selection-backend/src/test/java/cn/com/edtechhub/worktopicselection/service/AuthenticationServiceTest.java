package cn.com.edtechhub.worktopicselection.service;

import cn.com.edtechhub.worktopicselection.exception.BusinessException;
import cn.com.edtechhub.worktopicselection.exception.CodeBindMessageEnums;
import cn.com.edtechhub.worktopicselection.manager.satoken.AuthSessionManager;
import cn.com.edtechhub.worktopicselection.manager.security.SecurityRateLimitManager;
import cn.com.edtechhub.worktopicselection.model.entity.User;
import cn.com.edtechhub.worktopicselection.model.enums.UserRoleEnum;
import cn.com.edtechhub.worktopicselection.model.request.auth.LoginRequest;
import cn.com.edtechhub.worktopicselection.model.vo.LoginUserVO;
import cn.com.edtechhub.worktopicselection.service.impl.AuthenticationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 认证服务测试
 *
 * @author wobushi041
 */
class AuthenticationServiceTest {

    /**
     * 用户服务模拟依赖
     */
    private UserService userService;

    /**
     * 密码服务模拟依赖
     */
    private PasswordService passwordService;

    /**
     * 安全业务限频管理器模拟依赖
     */
    private SecurityRateLimitManager rateLimitManager;

    /**
     * 认证会话管理器模拟依赖
     */
    private AuthSessionManager sessionManager;

    /**
     * 待测试认证服务
     */
    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        passwordService = mock(PasswordService.class);
        rateLimitManager = mock(SecurityRateLimitManager.class);
        sessionManager = mock(AuthSessionManager.class);
        service = new AuthenticationServiceImpl(userService, passwordService, rateLimitManager, sessionManager);
    }

    // 场景：测试有效用户登录后创建会话并释放限频计数
    @Test
    void logsInActiveUserAndCreatesSession() {
        // 1. 准备有效教师账号和服务返回值
        LoginRequest request = loginRequest();
        User user = activeTeacher();
        LoginUserVO expected = new LoginUserVO();
        when(userService.userIsExist("teacher01")).thenReturn(user);
        when(passwordService.matchesPassword("password", "stored-hash")).thenReturn(true);
        when(passwordService.needsPasswordUpgrade("stored-hash")).thenReturn(false);
        when(userService.getLoginUserVO(user)).thenReturn(expected);

        // 2. 调用登录方法
        assertSame(expected, service.login(request, "203.0.113.10", "browser"));

        // 3. 断言限频、会话和释放流程均被执行
        verify(rateLimitManager).enforceLogin("teacher01", "203.0.113.10");
        verify(sessionManager).login(user, "browser");
        verify(rateLimitManager).releaseLogin("teacher01", "203.0.113.10");
    }

    // 场景：测试未知账号仍执行伪密码摘要校验
    @Test
    void unknownAccountStillPerformsDummyPasswordCheck() {
        // 1. 准备未知账号和伪密码校验结果
        LoginRequest request = loginRequest();
        when(userService.userIsExist("teacher01")).thenReturn(null);
        when(passwordService.matchesPassword(anyString(), anyString())).thenReturn(true);

        // 2. 调用登录方法
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.login(request, "203.0.113.10", "browser"));

        // 3. 断言返回统一参数错误且未创建会话
        assertSame(CodeBindMessageEnums.PARAMS_ERROR, exception.getCodeBindMessageEnums());
        verify(passwordService).matchesPassword(anyString(), anyString());
        verify(sessionManager, never()).login(org.mockito.ArgumentMatchers.any(), anyString());
    }

    // 场景：测试封禁账号在创建会话前被拒绝
    @Test
    void rejectsBannedAccountBeforeSessionCreation() {
        // 1. 准备密码正确的封禁账号
        LoginRequest request = loginRequest();
        User user = activeTeacher();
        user.setUserRole(UserRoleEnum.BAN_ROLE.getCode());
        when(userService.userIsExist("teacher01")).thenReturn(user);
        when(passwordService.matchesPassword("password", "stored-hash")).thenReturn(true);

        // 2. 调用登录方法
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.login(request, "203.0.113.10", "browser"));

        // 3. 断言返回无权限错误且未创建会话
        assertSame(CodeBindMessageEnums.NO_AUTH_ERROR, exception.getCodeBindMessageEnums());
        verify(sessionManager, never()).login(org.mockito.ArgumentMatchers.any(), anyString());
    }

    /**
     * 构建默认登录请求
     *
     * @return 登录请求
     */
    private static LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setAccount("teacher01");
        request.setPassword("password");
        return request;
    }

    /**
     * 构建有效教师用户
     *
     * @return 教师用户
     */
    private static User activeTeacher() {
        User user = new User();
        user.setId(8L);
        user.setUserAccount("teacher01");
        user.setUserPassword("stored-hash");
        user.setUserRole(UserRoleEnum.TEACHER.getCode());
        user.setStatus("老用户");
        return user;
    }

}
