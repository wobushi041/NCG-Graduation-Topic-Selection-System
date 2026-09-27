package cn.com.edtechhub.worktopicselection.service;

import cn.com.edtechhub.worktopicselection.exception.BusinessException;
import cn.com.edtechhub.worktopicselection.manager.redis.RedisManager;
import cn.com.edtechhub.worktopicselection.manager.security.SecurityRateLimitManager;
import cn.com.edtechhub.worktopicselection.model.vo.EmailVerificationVO;
import cn.com.edtechhub.worktopicselection.service.impl.VerificationCodeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VerificationCodeServiceTest {

    private UserService userService;
    private RedisManager redisManager;
    private MailService mailService;
    private SecurityRateLimitManager rateLimitManager;
    private VerificationCodeService service;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        redisManager = mock(RedisManager.class);
        mailService = mock(MailService.class);
        rateLimitManager = mock(SecurityRateLimitManager.class);
        service = new VerificationCodeServiceImpl(
                userService, redisManager, mailService, rateLimitManager,
                "qq.com,gmail.com,nfu.edu.cn"
        );
    }

    @Test
    void verificationCreatesOpaqueEmailBoundProofToken() {
        when(redisManager.consumeValue("email-captcha:user@qq.com", "123456")).thenReturn(1L);

        EmailVerificationVO result = service.verifyEmailCode(" User@QQ.com ", "123456");

        assertNotNull(result.getProofToken());
        assertFalse(result.getProofToken().contains("user@qq.com"));
        assertEquals(300L, result.getExpiresInSeconds());
        verify(redisManager).setValue(startsWith("email-proof:"), eq("user@qq.com"), eq(300L));
    }

    @Test
    void unknownResetAccountUsesGenericResponseWithoutSendingMail() {
        when(userService.getOne(any())).thenReturn(null);

        String response = service.sendPasswordResetCode("missing", "203.0.113.11");

        assertEquals("若账号存在且已绑定邮箱，临时密码将发送到该邮箱", response);
        verify(rateLimitManager).enforceMail(
                eq("password-reset-rate:missing"), eq("203.0.113.11"), anyString(), eq(false));
        verify(mailService, never()).sendCodeMail(anyString(), anyString(), anyString());
    }

    @Test
    void rejectsEmailDomainOutsideAllowList() {
        assertThrows(BusinessException.class,
                () -> service.sendEmailCode("user@example.com", "203.0.113.11"));
    }
}
