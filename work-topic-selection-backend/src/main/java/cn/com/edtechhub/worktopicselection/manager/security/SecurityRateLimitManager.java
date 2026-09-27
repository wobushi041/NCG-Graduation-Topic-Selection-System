package cn.com.edtechhub.worktopicselection.manager.security;

import cn.com.edtechhub.worktopicselection.exception.CodeBindMessageEnums;
import cn.com.edtechhub.worktopicselection.manager.redis.RedisManager;
import cn.com.edtechhub.worktopicselection.model.entity.User;
import cn.com.edtechhub.worktopicselection.utils.ThrowUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

/**
 * 账号、邮箱和客户端 IP 维度的 Redis 业务限频。
 */
@Component
public class SecurityRateLimitManager {

    private static final String LOGIN_ACCOUNT_PREFIX = "login-account-rate:";
    private static final String LOGIN_IP_PREFIX = "login-ip-rate:";
    private static final String MAIL_IP_PREFIX = "mail-ip-rate:";
    private static final String MAIL_GLOBAL_KEY = "mail-global-rate";
    private static final long LOGIN_WINDOW_SECONDS = 5 * 60;
    private static final long MAIL_TARGET_WINDOW_SECONDS = 60;
    private static final long MAIL_IP_WINDOW_SECONDS = 10 * 60;
    private static final long MAIL_GLOBAL_WINDOW_SECONDS = 60 * 60;

    private final RedisManager redisManager;

    public SecurityRateLimitManager(RedisManager redisManager) {
        this.redisManager = redisManager;
    }

    public void enforceLogin(String canonicalAccount, String clientIp) {
        String accountKey = LOGIN_ACCOUNT_PREFIX + canonicalAccount(canonicalAccount, null);
        String addressKey = LOGIN_IP_PREFIX + sanitizeAddress(clientIp);
        boolean accountAllowed = redisManager.tryAcquire(accountKey, 8, LOGIN_WINDOW_SECONDS);
        boolean addressAllowed = redisManager.tryAcquire(addressKey, 300, LOGIN_WINDOW_SECONDS);
        if (!accountAllowed || !addressAllowed) {
            if (accountAllowed) {
                redisManager.releaseRateLimit(accountKey);
            }
            if (addressAllowed) {
                redisManager.releaseRateLimit(addressKey);
            }
            throw new cn.com.edtechhub.worktopicselection.exception.BusinessException(
                    CodeBindMessageEnums.FLOW_RULES,
                    "登录尝试过于频繁，请 5 分钟后重试"
            );
        }
    }

    public void releaseLogin(String canonicalAccount, String clientIp) {
        redisManager.releaseRateLimit(LOGIN_ACCOUNT_PREFIX + canonicalAccount(canonicalAccount, null));
        redisManager.releaseRateLimit(LOGIN_IP_PREFIX + sanitizeAddress(clientIp));
    }

    public void enforceMail(String targetKey, String clientIp, String message) {
        enforceMail(targetKey, clientIp, message, true);
    }

    public void enforceMail(String targetKey, String clientIp, String message, boolean acquireGlobal) {
        boolean targetAllowed = redisManager.tryAcquire(targetKey, 1, MAIL_TARGET_WINDOW_SECONDS);
        ThrowUtils.throwIf(!targetAllowed, CodeBindMessageEnums.FLOW_RULES, message);
        String addressKey = MAIL_IP_PREFIX + sanitizeAddress(clientIp);
        boolean addressAllowed = redisManager.tryAcquire(addressKey, 100, MAIL_IP_WINDOW_SECONDS);
        if (!addressAllowed) {
            redisManager.releaseRateLimit(targetKey);
            ThrowUtils.throwIf(true, CodeBindMessageEnums.FLOW_RULES, message);
        }
        if (!acquireGlobal) {
            return;
        }
        boolean globalAllowed = redisManager.tryAcquire(MAIL_GLOBAL_KEY, 200, MAIL_GLOBAL_WINDOW_SECONDS);
        if (!globalAllowed) {
            redisManager.releaseRateLimit(targetKey);
            redisManager.releaseRateLimit(addressKey);
            ThrowUtils.throwIf(true, CodeBindMessageEnums.FLOW_RULES, message);
        }
    }

    public static String canonicalAccount(String requestedAccount, User user) {
        String account = user != null && StringUtils.isNotBlank(user.getUserAccount())
                ? user.getUserAccount() : requestedAccount;
        if (account == null) {
            return "unknown";
        }
        return Normalizer.normalize(account.trim(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }

    private static String sanitizeAddress(String clientIp) {
        if (StringUtils.isBlank(clientIp)) {
            return "unknown";
        }
        String normalized = clientIp.trim().replaceAll("[^0-9A-Fa-f:.]", "_");
        return normalized.length() <= 64 ? normalized : normalized.substring(0, 64);
    }
}
