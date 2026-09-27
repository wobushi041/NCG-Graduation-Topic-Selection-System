package cn.com.edtechhub.worktopicselection.service.impl;

import cn.com.edtechhub.worktopicselection.constant.UserConstant;
import cn.com.edtechhub.worktopicselection.event.CredentialsChangedEvent;
import cn.com.edtechhub.worktopicselection.exception.CodeBindMessageEnums;
import cn.com.edtechhub.worktopicselection.manager.security.SecurityRateLimitManager;
import cn.com.edtechhub.worktopicselection.model.dto.auth.AdminResetPasswordRequest;
import cn.com.edtechhub.worktopicselection.model.dto.auth.ChangePasswordRequest;
import cn.com.edtechhub.worktopicselection.model.dto.auth.ResetPasswordByCodeRequest;
import cn.com.edtechhub.worktopicselection.model.entity.User;
import cn.com.edtechhub.worktopicselection.model.vo.AdminResetPasswordVO;
import cn.com.edtechhub.worktopicselection.service.PasswordService;
import cn.com.edtechhub.worktopicselection.service.UserService;
import cn.com.edtechhub.worktopicselection.service.VerificationCodeService;
import cn.com.edtechhub.worktopicselection.utils.ThrowUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class PasswordServiceImpl implements PasswordService {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String UPPERCASE = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SPECIALS = "!@#$%*-_";
    private static final String TEMPORARY_PASSWORD_ALPHABET = UPPERCASE + LOWERCASE + DIGITS + SPECIALS;
    private static final int TEMPORARY_PASSWORD_LENGTH = 16;
    private static final String DUMMY_PASSWORD_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final UserService userService;
    private final VerificationCodeService verificationCodeService;
    private final SecurityRateLimitManager rateLimitManager;
    private final ApplicationEventPublisher eventPublisher;

    public PasswordServiceImpl(UserService userService, VerificationCodeService verificationCodeService,
                               SecurityRateLimitManager rateLimitManager,
                               ApplicationEventPublisher eventPublisher) {
        this.userService = userService;
        this.verificationCodeService = verificationCodeService;
        this.rateLimitManager = rateLimitManager;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public String encodePassword(String rawPassword) {
        if (!isPasswordValid(rawPassword)) {
            throw new IllegalArgumentException("密码长度必须为 8 到 72 个 UTF-8 字节");
        }
        return PASSWORD_ENCODER.encode(rawPassword);
    }

    @Override
    public String encodePasswordForMigration(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()
                || rawPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("旧密码无法迁移，请重置密码");
        }
        return PASSWORD_ENCODER.encode(rawPassword);
    }

    @Override
    public boolean matchesPassword(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        if (encodedPassword.startsWith("$2")) {
            try {
                return PASSWORD_ENCODER.matches(rawPassword, encodedPassword);
            } catch (IllegalArgumentException exception) {
                return false;
            }
        }
        if (!needsPasswordUpgrade(encodedPassword)) {
            return false;
        }
        String legacyPassword = DigestUtils.md5DigestAsHex(
                (UserConstant.LEGACY_PASSWORD_SALT + rawPassword).getBytes(StandardCharsets.UTF_8));
        return MessageDigest.isEqual(legacyPassword.getBytes(StandardCharsets.US_ASCII),
                encodedPassword.toLowerCase().getBytes(StandardCharsets.US_ASCII));
    }

    @Override
    public boolean needsPasswordUpgrade(String encodedPassword) {
        return encodedPassword != null && encodedPassword.matches("(?i)^[0-9a-f]{32}$");
    }

    @Override
    public String generateTemporaryPassword() {
        List<Character> characters = new ArrayList<>(TEMPORARY_PASSWORD_LENGTH);
        characters.add(randomCharacter(UPPERCASE));
        characters.add(randomCharacter(LOWERCASE));
        characters.add(randomCharacter(DIGITS));
        characters.add(randomCharacter(SPECIALS));
        while (characters.size() < TEMPORARY_PASSWORD_LENGTH) {
            characters.add(randomCharacter(TEMPORARY_PASSWORD_ALPHABET));
        }
        Collections.shuffle(characters, SECURE_RANDOM);
        StringBuilder password = new StringBuilder(TEMPORARY_PASSWORD_LENGTH);
        for (Character character : characters) {
            password.append(character);
        }
        return password.toString();
    }

    @Override
    public boolean isPasswordValid(String rawPassword) {
        if (rawPassword == null) {
            return false;
        }
        int bytes = rawPassword.getBytes(StandardCharsets.UTF_8).length;
        return bytes >= 8 && bytes <= 72;
    }

    @Override
    @Transactional
    public AdminResetPasswordVO adminReset(AdminResetPasswordRequest request) {
        User user = userService.getOne(new QueryWrapper<User>()
                .eq("userAccount", request.getAccount().trim())
                .eq("userName", request.getName().trim()));
        ThrowUtils.throwIf(user == null, CodeBindMessageEnums.NOT_FOUND_ERROR,
                "该用户不存在, 无需重置密码");
        ThrowUtils.throwIf(Long.valueOf(1L).equals(user.getId()), CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR,
                "禁止对超级管理员进行该操作");
        String temporaryPassword = generateTemporaryPassword();
        user.setUserPassword(encodePassword(temporaryPassword));
        user.setStatus("");
        ThrowUtils.throwIf(!userService.updateById(user), CodeBindMessageEnums.OPERATION_ERROR, "修改用户密码失败");
        eventPublisher.publishEvent(new CredentialsChangedEvent(user.getId()));
        return new AdminResetPasswordVO(user.getUserAccount(), temporaryPassword);
    }

    @Override
    @Transactional
    public Long changePassword(ChangePasswordRequest request, String clientIp) {
        String account = request.getAccount().trim();
        User user = userService.getOne(new QueryWrapper<User>().eq("userAccount", account));
        String canonical = SecurityRateLimitManager.canonicalAccount(account, user);
        rateLimitManager.enforceLogin(canonical, clientIp);
        boolean matches = user == null
                ? matchesPassword(request.getCurrentPassword(), DUMMY_PASSWORD_HASH) && false
                : matchesPassword(request.getCurrentPassword(), user.getUserPassword());
        ThrowUtils.throwIf(!matches, CodeBindMessageEnums.PARAMS_ERROR, "账号或旧密码错误");
        rateLimitManager.releaseLogin(canonical, clientIp);
        ThrowUtils.throwIf(StringUtils.isBlank(user.getStatus()) && needsPasswordUpgrade(user.getUserPassword()),
                CodeBindMessageEnums.USER_INIT_PASSWD, "旧版初始账号需要管理员先重置为随机临时密码");

        String email = verificationCodeService.normalizeEmail(request.getEmail());
        if (StringUtils.isNotBlank(email)) {
            if (StringUtils.isBlank(user.getEmail())) {
                ThrowUtils.throwIf(!verificationCodeService.consumeEmailProof(email, request.getEmailProofToken()),
                        CodeBindMessageEnums.PARAMS_ERROR, "邮箱验证凭证无效或已过期，请重新验证");
                user.setEmail(email);
            } else {
                ThrowUtils.throwIf(!verificationCodeService.normalizeEmail(user.getEmail()).equals(email),
                        CodeBindMessageEnums.PARAMS_ERROR, "用户已绑定其他邮箱；更换邮箱请联系系统管理员");
            }
        }
        updatePassword(user, request.getNewPassword());
        return user.getId();
    }

    @Override
    @Transactional
    public Long resetPassword(ResetPasswordByCodeRequest request) {
        String account = request.getAccount().trim();
        User user = userService.getOne(new QueryWrapper<User>().eq("userAccount", account));
        String canonicalAccount = user == null ? account : user.getUserAccount();
        long consumeResult = verificationCodeService.consumePasswordResetCode(canonicalAccount, request.getResetCode());
        ThrowUtils.throwIf(consumeResult == 0, CodeBindMessageEnums.NOT_FOUND_ERROR,
                "重置码已过期，请重新获取");
        ThrowUtils.throwIf(consumeResult != 1 || user == null, CodeBindMessageEnums.PARAMS_ERROR,
                "账号或重置码错误");
        updatePassword(user, request.getNewPassword());
        return user.getId();
    }

    private void updatePassword(User user, String newPassword) {
        user.setUserPassword(encodePassword(newPassword));
        user.setStatus("老用户");
        ThrowUtils.throwIf(!userService.updateById(user), CodeBindMessageEnums.OPERATION_ERROR, "修改密码失败");
        eventPublisher.publishEvent(new CredentialsChangedEvent(user.getId()));
    }

    private static char randomCharacter(String characters) {
        return characters.charAt(SECURE_RANDOM.nextInt(characters.length()));
    }
}
