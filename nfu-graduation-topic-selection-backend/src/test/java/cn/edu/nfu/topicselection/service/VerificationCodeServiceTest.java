package cn.edu.nfu.topicselection.service;

import cn.edu.nfu.topicselection.exception.BusinessException;
import cn.edu.nfu.topicselection.manager.redis.RedisManager;
import cn.edu.nfu.topicselection.manager.security.SecurityRateLimitManager;
import cn.edu.nfu.topicselection.model.vo.EmailVerificationVO;
import cn.edu.nfu.topicselection.service.impl.VerificationCodeServiceImpl;
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

/**
 * 验证码服务测试
 *
 * @author wobushi041
 */
class VerificationCodeServiceTest {

    /**
     * 用户服务模拟依赖
     */
    private UserService userService;

    /**
     * Redis 管理器模拟依赖
     */
    private RedisManager redisManager;

    /**
     * 邮件服务模拟依赖
     */
    private MailService mailService;

    /**
     * 安全业务限频管理器模拟依赖
     */
    private SecurityRateLimitManager rateLimitManager;

    /**
     * 待测试验证码服务
     */
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

    // 场景：测试邮箱验证码签发不暴露邮箱的一次性凭证
    @Test
    void verificationCreatesOpaqueEmailBoundProofToken() {
        // 1. 准备邮箱验证码原子消费成功结果
        when(redisManager.consumeValue("email-captcha:user@qq.com", "123456")).thenReturn(1L);

        // 2. 调用邮箱验证码校验方法
        EmailVerificationVO result = service.verifyEmailCode(" User@QQ.com ", "123456");

        // 3. 断言凭证不可推断邮箱且按预期写入 Redis
        assertNotNull(result.getProofToken());
        assertFalse(result.getProofToken().contains("user@qq.com"));
        assertEquals(300L, result.getExpiresInSeconds());
        verify(redisManager).setValue(startsWith("email-proof:"), eq("user@qq.com"), eq(300L));
    }

    // 场景：测试未知账号返回通用消息且不发送邮件
    @Test
    void unknownResetAccountUsesGenericResponseWithoutSendingMail() {
        // 1. 准备账号不存在的查询结果
        when(userService.getOne(any())).thenReturn(null);

        // 2. 调用发送密码重置码方法
        String response = service.sendPasswordResetCode("missing", "203.0.113.11");

        // 3. 断言响应不暴露账号状态且不调用邮件服务
        assertEquals("若账号存在且已绑定邮箱，临时密码将发送到该邮箱", response);
        verify(rateLimitManager).enforceMail(
                eq("password-reset-rate:missing"), eq("203.0.113.11"), anyString(), eq(false));
        verify(mailService, never()).sendCodeMail(anyString(), anyString(), anyString());
    }

    // 场景：测试邮箱域名不在白名单时拒绝发送验证码
    @Test
    void rejectsEmailDomainOutsideAllowList() {
        // 1. 准备白名单外的邮箱地址

        // 2. 调用发送邮箱验证码方法

        // 3. 断言抛出统一业务异常
        assertThrows(BusinessException.class,
                () -> service.sendEmailCode("user@example.com", "203.0.113.11"));
    }

}
