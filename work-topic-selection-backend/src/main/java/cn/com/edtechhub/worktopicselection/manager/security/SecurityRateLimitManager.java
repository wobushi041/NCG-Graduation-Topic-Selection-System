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
 * 账号、邮箱和客户端 IP 维度的 Redis 业务限频
 *
 * @author wobushi041
 */
@Component
public class SecurityRateLimitManager {

    /**
     * 登录账号限频键前缀
     */
    private static final String LOGIN_ACCOUNT_PREFIX = "login-account-rate:";

    /**
     * 登录 IP 限频键前缀
     */
    private static final String LOGIN_IP_PREFIX = "login-ip-rate:";

    /**
     * 邮件 IP 限频键前缀
     */
    private static final String MAIL_IP_PREFIX = "mail-ip-rate:";

    /**
     * 全局邮件限频键
     */
    private static final String MAIL_GLOBAL_KEY = "mail-global-rate";

    /**
     * 登录限频窗口秒数
     */
    private static final long LOGIN_WINDOW_SECONDS = 5 * 60;

    /**
     * 单个邮件目标限频窗口秒数
     */
    private static final long MAIL_TARGET_WINDOW_SECONDS = 60;

    /**
     * 邮件 IP 限频窗口秒数
     */
    private static final long MAIL_IP_WINDOW_SECONDS = 10 * 60;

    /**
     * 全局邮件限频窗口秒数
     */
    private static final long MAIL_GLOBAL_WINDOW_SECONDS = 60 * 60;

    /**
     * 注入 Redis 管理器依赖
     */
    private final RedisManager redisManager;

    /**
     * 初始化安全业务限频管理器
     *
     * @param redisManager Redis 管理器
     */
    public SecurityRateLimitManager(RedisManager redisManager) {
        this.redisManager = redisManager;
    }

    /// 登录限频 ///

    /**
     * 同时校验账号和客户端 IP 的登录频率
     *
     * @param canonicalAccount 规范化账号
     * @param clientIp         客户端 IP
     */
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

    /**
     * 释放账号和客户端 IP 的本次登录限频计数
     *
     * @param canonicalAccount 规范化账号
     * @param clientIp         客户端 IP
     */
    public void releaseLogin(String canonicalAccount, String clientIp) {
        redisManager.releaseRateLimit(LOGIN_ACCOUNT_PREFIX + canonicalAccount(canonicalAccount, null));
        redisManager.releaseRateLimit(LOGIN_IP_PREFIX + sanitizeAddress(clientIp));
    }

    /// 邮件限频 ///

    /**
     * 校验邮件目标、客户端 IP 和全局发送频率
     *
     * @param targetKey 邮件目标限频键
     * @param clientIp  客户端 IP
     * @param message   限频提示信息
     */
    public void enforceMail(String targetKey, String clientIp, String message) {
        enforceMail(targetKey, clientIp, message, true);
    }

    /**
     * 按需校验邮件目标、客户端 IP 和全局发送频率
     *
     * @param targetKey     邮件目标限频键
     * @param clientIp      客户端 IP
     * @param message       限频提示信息
     * @param acquireGlobal 是否占用全局邮件限频额度
     */
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

    /// 标识规范化 ///

    /**
     * 优先使用数据库账号并生成统一的限频账号标识
     *
     * @param requestedAccount 请求账号
     * @param user             已查询到的用户
     * @return 规范化账号标识
     */
    public static String canonicalAccount(String requestedAccount, User user) {
        String account = user != null && StringUtils.isNotBlank(user.getUserAccount())
                ? user.getUserAccount() : requestedAccount;
        if (account == null) {
            return "unknown";
        }
        return Normalizer.normalize(account.trim(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }

    /**
     * 清理客户端 IP 中不适合写入 Redis 键的字符
     *
     * @param clientIp 客户端 IP
     * @return 安全的客户端地址标识
     */
    private static String sanitizeAddress(String clientIp) {
        if (StringUtils.isBlank(clientIp)) {
            return "unknown";
        }
        String normalized = clientIp.trim().replaceAll("[^0-9A-Fa-f:.]", "_");
        return normalized.length() <= 64 ? normalized : normalized.substring(0, 64);
    }

}
