package cn.com.edtechhub.worktopicselection.service.impl;

import cn.com.edtechhub.worktopicselection.exception.CodeBindMessageEnums;
import cn.com.edtechhub.worktopicselection.manager.redis.RedisManager;
import cn.com.edtechhub.worktopicselection.manager.security.SecurityRateLimitManager;
import cn.com.edtechhub.worktopicselection.model.entity.User;
import cn.com.edtechhub.worktopicselection.model.vo.EmailVerificationVO;
import cn.com.edtechhub.worktopicselection.service.MailService;
import cn.com.edtechhub.worktopicselection.service.UserService;
import cn.com.edtechhub.worktopicselection.service.VerificationCodeService;
import cn.com.edtechhub.worktopicselection.utils.ThrowUtils;
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

@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {

    private static final String PASSWORD_RESET_CODE_PREFIX = "password-reset:";
    private static final String PASSWORD_RESET_RATE_PREFIX = "password-reset-rate:";
    private static final String EMAIL_CODE_PREFIX = "email-captcha:";
    private static final String EMAIL_RATE_PREFIX = "email-captcha-rate:";
    private static final String EMAIL_PROOF_PREFIX = "email-proof:";
    private static final long CODE_TTL_SECONDS = 2 * 60;
    private static final long PROOF_TTL_SECONDS = 5 * 60;
    private static final String RESET_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final String CAPTCHA_ALPHABET = "23456789";

    private final SecureRandom secureRandom = new SecureRandom();
    private final UserService userService;
    private final RedisManager redisManager;
    private final MailService mailService;
    private final SecurityRateLimitManager rateLimitManager;
    private final Set<String> allowedEmailDomains;

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
            mailService.sendCodeMail(normalizeEmail(user.getEmail()), "毕业设计选题系统", code);
        } catch (RuntimeException exception) {
            redisManager.deleteKey(key);
            throw exception;
        }
        return response;
    }

    @Override
    public String sendEmailCode(String email, String clientIp) {
        String normalizedEmail = requireAllowedEmail(email);
        rateLimitManager.enforceMail(EMAIL_RATE_PREFIX + normalizedEmail, clientIp,
                "验证码发送过于频繁，请稍后重试");
        String code = generateCode(6, CAPTCHA_ALPHABET);
        String key = EMAIL_CODE_PREFIX + normalizedEmail;
        redisManager.setValue(key, code, CODE_TTL_SECONDS);
        try {
            mailService.sendCaptchaMail(normalizedEmail, "毕业设计选题系统", code);
        } catch (RuntimeException exception) {
            redisManager.deleteKey(key);
            throw exception;
        }
        return "发送成功，请查收邮箱";
    }

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

    @Override
    public boolean consumeEmailProof(String email, String proofToken) {
        if (StringUtils.isBlank(proofToken)) {
            return false;
        }
        String normalizedEmail = normalizeEmail(email);
        return redisManager.consumeValue(EMAIL_PROOF_PREFIX + sha256(proofToken), normalizedEmail) == 1;
    }

    @Override
    public long consumePasswordResetCode(String account, String resetCode) {
        return redisManager.consumeValue(PASSWORD_RESET_CODE_PREFIX + account, resetCode);
    }

    @Override
    public String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String requireAllowedEmail(String email) {
        String normalized = normalizeEmail(email);
        int at = normalized == null ? -1 : normalized.lastIndexOf('@');
        String domain = at < 0 ? "" : normalized.substring(at + 1);
        ThrowUtils.throwIf(!allowedEmailDomains.contains(domain), CodeBindMessageEnums.PARAMS_ERROR,
                "本系统不支持该邮箱域名");
        return normalized;
    }

    private String generateCode(int length, String alphabet) {
        StringBuilder value = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            value.append(alphabet.charAt(secureRandom.nextInt(alphabet.length())));
        }
        return value.toString();
    }

    private String generateProofToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

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
