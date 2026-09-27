package cn.com.edtechhub.worktopicselection.manager.security;

import cn.com.edtechhub.worktopicselection.exception.BusinessException;
import cn.com.edtechhub.worktopicselection.exception.CodeBindMessageEnums;
import cn.com.edtechhub.worktopicselection.manager.redis.RedisManager;
import cn.com.edtechhub.worktopicselection.model.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecurityRateLimitManagerTest {

    @Test
    void canonicalAccountPrefersDatabaseValue() {
        User user = new User();
        user.setUserAccount("resume");
        assertEquals("resume", SecurityRateLimitManager.canonicalAccount("résumé", user));
    }

    @Test
    void loginLimitUsesAccountAndIpAndReleasesPartialPermit() {
        RedisManager redis = mock(RedisManager.class);
        when(redis.tryAcquire("login-account-rate:student01", 8, 300)).thenReturn(true);
        when(redis.tryAcquire("login-ip-rate:203.0.113.10", 300, 300)).thenReturn(false);
        SecurityRateLimitManager manager = new SecurityRateLimitManager(redis);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> manager.enforceLogin("Student01", "203.0.113.10"));

        assertSame(CodeBindMessageEnums.FLOW_RULES, exception.getCodeBindMessageEnums());
        verify(redis).releaseRateLimit("login-account-rate:student01");
    }
}
