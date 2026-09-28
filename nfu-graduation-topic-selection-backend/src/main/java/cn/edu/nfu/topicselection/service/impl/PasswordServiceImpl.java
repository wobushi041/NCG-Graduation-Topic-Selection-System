package cn.edu.nfu.topicselection.service.impl;

import cn.edu.nfu.topicselection.constant.UserConstant;
import cn.edu.nfu.topicselection.event.CredentialsChangedEvent;
import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import cn.edu.nfu.topicselection.manager.security.SecurityRateLimitManager;
import cn.edu.nfu.topicselection.model.entity.User;
import cn.edu.nfu.topicselection.model.request.auth.AdminResetPasswordRequest;
import cn.edu.nfu.topicselection.model.request.auth.ChangePasswordRequest;
import cn.edu.nfu.topicselection.model.request.auth.ResetPasswordByCodeRequest;
import cn.edu.nfu.topicselection.model.vo.AdminResetPasswordVO;
import cn.edu.nfu.topicselection.service.PasswordService;
import cn.edu.nfu.topicselection.service.UserService;
import cn.edu.nfu.topicselection.service.VerificationCodeService;
import cn.edu.nfu.topicselection.utils.ThrowUtils;
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

/**
 * 基于 BCrypt、旧 MD5 兼容校验和事务事件实现密码业务
 *
 * @author wobushi041
 */
@Service
public class PasswordServiceImpl implements PasswordService {

    /**
     * BCrypt 密码编码器
     */
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    /**
     * 临时密码安全随机数生成器
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 临时密码大写字母集合
     */
    private static final String UPPERCASE = "ABCDEFGHJKLMNPQRSTUVWXYZ";

    /**
     * 临时密码小写字母集合
     */
    private static final String LOWERCASE = "abcdefghijkmnopqrstuvwxyz";

    /**
     * 临时密码数字集合
     */
    private static final String DIGITS = "23456789";

    /**
     * 临时密码特殊字符集合
     */
    private static final String SPECIALS = "!@#$%*-_";

    /**
     * 临时密码完整字符集合
     */
    private static final String TEMPORARY_PASSWORD_ALPHABET = UPPERCASE + LOWERCASE + DIGITS + SPECIALS;

    /**
     * 临时密码字符长度
     */
    private static final int TEMPORARY_PASSWORD_LENGTH = 16;

    /**
     * 未知账号密码校验使用的伪 BCrypt 摘要
     */
    private static final String DUMMY_PASSWORD_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    /**
     * 注入用户服务依赖
     */
    private final UserService userService;

    /**
     * 注入验证码服务依赖
     */
    private final VerificationCodeService verificationCodeService;

    /**
     * 注入安全业务限频管理器依赖
     */
    private final SecurityRateLimitManager rateLimitManager;

    /**
     * 注入应用事件发布器依赖
     */
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 初始化密码服务实现
     *
     * @param userService             用户服务
     * @param verificationCodeService 验证码服务
     * @param rateLimitManager        安全业务限频管理器
     * @param eventPublisher          应用事件发布器
     */
    public PasswordServiceImpl(UserService userService, VerificationCodeService verificationCodeService,
                               SecurityRateLimitManager rateLimitManager,
                               ApplicationEventPublisher eventPublisher) {
        this.userService = userService;
        this.verificationCodeService = verificationCodeService;
        this.rateLimitManager = rateLimitManager;
        this.eventPublisher = eventPublisher;
    }

    /// 密码编码与校验 ///

    /**
     * 校验 UTF-8 字节长度后使用 BCrypt 编码密码
     *
     * @param rawPassword 原始密码
     * @return BCrypt 密码摘要
     */
    @Override
    public String encodePassword(String rawPassword) {
        if (!isPasswordValid(rawPassword)) {
            throw new IllegalArgumentException("密码长度必须为 8 到 72 个 UTF-8 字节");
        }
        return PASSWORD_ENCODER.encode(rawPassword);
    }

    /**
     * 对符合迁移上限的旧密码使用 BCrypt 重新编码
     *
     * @param rawPassword 原始密码
     * @return BCrypt 密码摘要
     */
    @Override
    public String encodePasswordForMigration(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()
                || rawPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("旧密码无法迁移，请重置密码");
        }
        return PASSWORD_ENCODER.encode(rawPassword);
    }

    /**
     * 按 BCrypt 或旧 MD5 格式校验原始密码
     *
     * @param rawPassword     原始密码
     * @param encodedPassword 已编码密码
     * @return 密码是否匹配
     */
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

    /**
     * 通过 MD5 摘要格式判断密码是否需要升级
     *
     * @param encodedPassword 已编码密码
     * @return 是否需要升级
     */
    @Override
    public boolean needsPasswordUpgrade(String encodedPassword) {
        return encodedPassword != null && encodedPassword.matches("(?i)^[0-9a-f]{32}$");
    }

    /// 临时密码 ///

    /**
     * 使用 SecureRandom 生成并打乱符合复杂度要求的临时密码
     *
     * @return 随机临时密码
     */
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

    /**
     * 按 UTF-8 字节数校验密码长度
     *
     * @param rawPassword 原始密码
     * @return 密码是否有效
     */
    @Override
    public boolean isPasswordValid(String rawPassword) {
        if (rawPassword == null) {
            return false;
        }
        int bytes = rawPassword.getBytes(StandardCharsets.UTF_8).length;
        return bytes >= 8 && bytes <= 72;
    }

    /// 密码变更 ///

    /**
     * 查询目标账号、生成临时密码并在事务提交后注销用户会话
     *
     * @param request 管理员重置密码请求
     * @return 账号与临时密码
     */
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

    /**
     * 校验当前密码和邮箱凭证后更新 BCrypt 密码
     *
     * @param request  修改密码请求
     * @param clientIp 客户端 IP
     * @return 用户 id
     */
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

    /**
     * 原子消费 Redis 重置码后更新 BCrypt 密码
     *
     * @param request 重置密码请求
     * @return 用户 id
     */
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

    /**
     * 持久化新密码并发布凭证变更事务事件
     *
     * @param user        目标用户
     * @param newPassword 新密码
     */
    private void updatePassword(User user, String newPassword) {
        user.setUserPassword(encodePassword(newPassword));
        user.setStatus("老用户");
        ThrowUtils.throwIf(!userService.updateById(user), CodeBindMessageEnums.OPERATION_ERROR, "修改密码失败");
        eventPublisher.publishEvent(new CredentialsChangedEvent(user.getId()));
    }

    /**
     * 从指定字符集合中随机选择一个字符
     *
     * @param characters 候选字符集合
     * @return 随机字符
     */
    private static char randomCharacter(String characters) {
        return characters.charAt(SECURE_RANDOM.nextInt(characters.length()));
    }

}
