package cn.edu.nfu.topicselection.manager.security;

import cn.edu.nfu.topicselection.exception.BusinessException;
import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import cn.edu.nfu.topicselection.manager.redis.RedisManager;
import cn.edu.nfu.topicselection.model.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 安全业务限频管理器测试
 *
 * @author wobushi041
 */
class SecurityRateLimitManagerTest {

    // 场景：测试限频账号优先使用数据库中的规范账号
    @Test
    void canonicalAccountPrefersDatabaseValue() {
        // 1. 准备包含规范账号的用户
        User user = new User();
        user.setUserAccount("resume");

        // 2. 调用账号规范化方法

        // 3. 断言返回数据库中的账号
        assertEquals("resume", SecurityRateLimitManager.canonicalAccount("résumé", user));
    }

    // 场景：测试登录 IP 超限时释放已经获取的账号额度
    @Test
    void loginLimitUsesAccountAndIpAndReleasesPartialPermit() {
        // 1. 准备账号通过而 IP 被拒绝的 Redis 限频结果
        RedisManager redis = mock(RedisManager.class);
        when(redis.tryAcquire("login-account-rate:student01", 8, 300)).thenReturn(true);
        when(redis.tryAcquire("login-ip-rate:203.0.113.10", 300, 300)).thenReturn(false);
        SecurityRateLimitManager manager = new SecurityRateLimitManager(redis);

        // 2. 调用登录限频检查
        BusinessException exception = assertThrows(BusinessException.class,
                () -> manager.enforceLogin("Student01", "203.0.113.10"));

        // 3. 断言返回限流错误并释放账号额度
        assertSame(CodeBindMessageEnums.FLOW_RULES, exception.getCodeBindMessageEnums());
        verify(redis).releaseRateLimit("login-account-rate:student01");
    }

}
