package cn.com.edtechhub.worktopicselection.service;

import cn.com.edtechhub.worktopicselection.exception.BusinessException;
import cn.com.edtechhub.worktopicselection.exception.CodeBindMessageEnums;
import cn.com.edtechhub.worktopicselection.manager.satoken.AuthSessionManager;
import cn.com.edtechhub.worktopicselection.manager.security.SecurityRateLimitManager;
import cn.com.edtechhub.worktopicselection.model.dto.auth.LoginRequest;
import cn.com.edtechhub.worktopicselection.model.entity.User;
import cn.com.edtechhub.worktopicselection.model.enums.UserRoleEnum;
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

class AuthenticationServiceTest {

    private UserService userService;
    private PasswordService passwordService;
    private SecurityRateLimitManager rateLimitManager;
    private AuthSessionManager sessionManager;
    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        passwordService = mock(PasswordService.class);
        rateLimitManager = mock(SecurityRateLimitManager.class);
        sessionManager = mock(AuthSessionManager.class);
        service = new AuthenticationServiceImpl(userService, passwordService, rateLimitManager, sessionManager);
    }

    @Test
    void logsInActiveUserAndCreatesSession() {
        LoginRequest request = loginRequest();
        User user = activeTeacher();
        LoginUserVO expected = new LoginUserVO();
        when(userService.userIsExist("teacher01")).thenReturn(user);
        when(passwordService.matchesPassword("password", "stored-hash")).thenReturn(true);
        when(passwordService.needsPasswordUpgrade("stored-hash")).thenReturn(false);
        when(userService.getLoginUserVO(user)).thenReturn(expected);

        assertSame(expected, service.login(request, "203.0.113.10", "browser"));

        verify(rateLimitManager).enforceLogin("teacher01", "203.0.113.10");
        verify(sessionManager).login(user, "browser");
        verify(rateLimitManager).releaseLogin("teacher01", "203.0.113.10");
    }

    @Test
    void unknownAccountStillPerformsDummyPasswordCheck() {
        LoginRequest request = loginRequest();
        when(userService.userIsExist("teacher01")).thenReturn(null);
        when(passwordService.matchesPassword(anyString(), anyString())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.login(request, "203.0.113.10", "browser"));

        assertSame(CodeBindMessageEnums.PARAMS_ERROR, exception.getCodeBindMessageEnums());
        verify(passwordService).matchesPassword(anyString(), anyString());
        verify(sessionManager, never()).login(org.mockito.ArgumentMatchers.any(), anyString());
    }

    @Test
    void rejectsBannedAccountBeforeSessionCreation() {
        LoginRequest request = loginRequest();
        User user = activeTeacher();
        user.setUserRole(UserRoleEnum.BAN_ROLE.getCode());
        when(userService.userIsExist("teacher01")).thenReturn(user);
        when(passwordService.matchesPassword("password", "stored-hash")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.login(request, "203.0.113.10", "browser"));

        assertSame(CodeBindMessageEnums.NO_AUTH_ERROR, exception.getCodeBindMessageEnums());
        verify(sessionManager, never()).login(org.mockito.ArgumentMatchers.any(), anyString());
    }

    private static LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setAccount("teacher01");
        request.setPassword("password");
        return request;
    }

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
