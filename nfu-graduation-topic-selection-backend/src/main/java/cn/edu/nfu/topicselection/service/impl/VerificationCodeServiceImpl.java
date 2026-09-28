package cn.edu.nfu.topicselection.service.impl;

import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import cn.edu.nfu.topicselection.manager.redis.RedisManager;
import cn.edu.nfu.topicselection.manager.security.SecurityRateLimitManager;
import cn.edu.nfu.topicselection.model.entity.User;
import cn.edu.nfu.topicselection.model.vo.EmailVerificationVO;
import cn.edu.nfu.topicselection.service.MailService;
import cn.edu.nfu.topicselection.service.UserService;
import cn.edu.nfu.topicselection.service.VerificationCodeService;
import cn.edu.nfu.topicselection.utils.ThrowUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 基于 Redis 一次性消费和邮件服务实现验证码业务
 *
 * @author wobushi041
 */
@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {

    /**
     * 密码重置码 Redis 键前缀
     */
    private static final String PASSWORD_RESET_CODE_PREFIX = "password-reset:";

    /**
     * 密码重置码限频 Redis 键前缀
     */
    private static final String PASSWORD_RESET_RATE_PREFIX = "password-reset-rate:";

    /**
     * 邮箱验证码 Redis 键前缀
     */
    private static final String EMAIL_CODE_PREFIX = "email-captcha:";

    /**
     * 邮箱验证码限频 Redis 键前缀
     */
    private static final String EMAIL_RATE_PREFIX = "email-captcha-rate:";

    /**
     * 邮箱验证凭证 Redis 键前缀
     */
    private static final String EMAIL_PROOF_PREFIX = "email-proof:";

    /**
     * 验证码有效秒数
     */
    private static final long CODE_TTL_SECONDS = 2 * 60;

    /**
     * 邮箱验证凭证有效秒数
     */
    private static final long PROOF_TTL_SECONDS = 5 * 60;

    /**
     * 密码重置码字符集合
     */
    private static final String RESET_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    /**
     * 邮箱验证码字符集合
     */
    private static final String CAPTCHA_ALPHABET = "23456789";

    /**
     * 验证码安全随机数生成器
     */
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 注入用户服务依赖
     */
    private final UserService userService;

    /**
     * 注入 Redis 管理器依赖
     */
    private final RedisManager redisManager;

    /**
     * 注入邮件服务依赖
     */
    private final MailService mailService;

    /**
     * 注入安全业务限频管理器依赖
     */
    private final SecurityRateLimitManager rateLimitManager;

    /**
     * 允许使用的邮箱域名集合
     */
    private final Set<String> allowedEmailDomains;

    /**
     * 初始化验证码服务并解析邮箱域名白名单
     *
     * @param userService      用户服务
     * @param redisManager     Redis 管理器
     * @param mailService      邮件服务
     * @param rateLimitManager 安全业务限频管理器
     * @param domains          邮箱域名白名单配置
     */
    public VerificationCodeServiceImpl(UserService userService, RedisManager redisManager,
                                       MailService mailService, SecurityRateLimitManager rateLimitManager,
                                       @Value("${app.security.allowed-email-domains:qq.com,gmail.com,nfu.edu.cn}") String domains) {
        this.userService = userService;
        this.redisManager = redisManager;
        this.mailService = mailService;
        this.rateLimitManager = rateLimitManager;
        this.allowedEmailDomains = new HashSet<>();
        Arrays.stream(domains.split(","))
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .filter(StringUtils::isNotBlank)
                .forEach(this.allowedEmailDomains::add);
    }

    /// 密码重置码 ///

    /**
     * 将密码重置码写入 Redis 并发送到账号绑定邮箱
     *
     * @param account  账号
     * @param clientIp 客户端 IP
     * @return 防止账号枚举的通用发送结果
     */
    @Override
    public String sendPasswordResetCode(String account, String clientIp) {
        String normalizedAccount = account.trim();
        User user = userService.getOne(new QueryWrapper<User>().eq("userAccount", normalizedAccount));
        String canonical = SecurityRateLimitManager.canonicalAccount(normalizedAccount, user);
        boolean canSend = user != null && StringUtils.isNotBlank(user.getEmail());
        rateLimitManager.enforceMail(PASSWORD_RESET_RATE_PREFIX + canonical, clientIp,
                "临时密码发送过于频繁，请稍后重试", canSend);
        String response = "若账号存在且已绑定邮箱，临时密码将发送到该邮箱";
        if (!canSend) {
            return response;
        }
        String code = generateCode(12, RESET_ALPHABET);
        String key = PASSWORD_RESET_CODE_PREFIX + user.getUserAccount();
        redisManager.setValue(key, code, CODE_TTL_SECONDS);
        try {
            mailService.sendCodeMail(normalizeEmail(user.getEmail()), "广州南方学院毕设选题管理系统", code);
        } catch (RuntimeException exception) {
            redisManager.deleteKey(key);
            throw exception;
        }
        return response;
    }

    /// 邮箱验证码 ///

    /**
     * 将邮箱验证码写入 Redis 并通过邮件服务发送
     *
     * @param email    邮箱
     * @param clientIp 客户端 IP
     * @return 发送结果
     */
    @Override
    public String sendEmailCode(String email, String clientIp) {
        String normalizedEmail = requireAllowedEmail(email);
        rateLimitManager.enforceMail(EMAIL_RATE_PREFIX + normalizedEmail, clientIp,
                "验证码发送过于频繁，请稍后重试");
        String code = generateCode(6, CAPTCHA_ALPHABET);
        String key = EMAIL_CODE_PREFIX + normalizedEmail;
        redisManager.setValue(key, code, CODE_TTL_SECONDS);
        try {
            mailService.sendCaptchaMail(normalizedEmail, "广州南方学院毕设选题管理系统", code);
        } catch (RuntimeException exception) {
            redisManager.deleteKey(key);
            throw exception;
        }
        return "发送成功，请查收邮箱";
    }

    /**
     * 原子消费 Redis 邮箱验证码并签发一次性验证凭证
     *
     * @param email 邮箱
     * @param code  验证码
     * @return 邮箱验证凭证
     */
    @Override
    public EmailVerificationVO verifyEmailCode(String email, String code) {
        String normalizedEmail = requireAllowedEmail(email);
        long result = redisManager.consumeValue(EMAIL_CODE_PREFIX + normalizedEmail, code);
        ThrowUtils.throwIf(result == 0, CodeBindMessageEnums.NOT_FOUND_ERROR, "请先发送验证码或验证码已过期");
        ThrowUtils.throwIf(result != 1, CodeBindMessageEnums.PARAMS_ERROR, "验证码错误，请重新获取");
        String token = generateProofToken();
        redisManager.setValue(EMAIL_PROOF_PREFIX + sha256(token), normalizedEmail, PROOF_TTL_SECONDS);
        return new EmailVerificationVO(token, PROOF_TTL_SECONDS);
    }

    /**
     * 按邮箱匹配并原子消费 Redis 验证凭证
     *
     * @param email      邮箱
     * @param proofToken 邮箱验证凭证
     * @return 凭证是否有效并成功消费
     */
    @Override
    public boolean consumeEmailProof(String email, String proofToken) {
        if (StringUtils.isBlank(proofToken)) {
            return false;
        }
        String normalizedEmail = normalizeEmail(email);
        return redisManager.consumeValue(EMAIL_PROOF_PREFIX + sha256(proofToken), normalizedEmail) == 1;
    }

    /**
     * 按账号匹配并原子消费 Redis 密码重置码
     *
     * @param account   账号
     * @param resetCode 密码重置码
     * @return Redis 原子消费结果
     */
    @Override
    public long consumePasswordResetCode(String account, String resetCode) {
        return redisManager.consumeValue(PASSWORD_RESET_CODE_PREFIX + account, resetCode);
    }

    /// 数据规范化 ///

    /**
     * 去除邮箱首尾空白并转换为小写
     *
     * @param email 邮箱
     * @return 规范化邮箱地址
     */
    @Override
    public String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 校验邮箱域名是否位于配置白名单
     *
     * @param email 邮箱
     * @return 规范化邮箱地址
     */
    private String requireAllowedEmail(String email) {
        String normalized = normalizeEmail(email);
        int at = normalized == null ? -1 : normalized.lastIndexOf('@');
        String domain = at < 0 ? "" : normalized.substring(at + 1);
        ThrowUtils.throwIf(!allowedEmailDomains.contains(domain), CodeBindMessageEnums.PARAMS_ERROR,
                "本系统不支持该邮箱域名");
        return normalized;
    }

    /// 凭证生成 ///

    /**
     * 从指定字符集合生成安全随机码
     *
     * @param length   随机码长度
     * @param alphabet 候选字符集合
     * @return 安全随机码
     */
    private String generateCode(int length, String alphabet) {
        StringBuilder value = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            value.append(alphabet.charAt(secureRandom.nextInt(alphabet.length())));
        }
        return value.toString();
    }

    /**
     * 生成 URL 安全的一次性邮箱验证凭证
     *
     * @return 邮箱验证凭证
     */
    private String generateProofToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 计算凭证的 SHA-256 十六进制摘要
     *
     * @param value 原始凭证
     * @return SHA-256 十六进制摘要
     */
    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                hex.append(String.format("%02x", item));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", exception);
        }
    }

}
