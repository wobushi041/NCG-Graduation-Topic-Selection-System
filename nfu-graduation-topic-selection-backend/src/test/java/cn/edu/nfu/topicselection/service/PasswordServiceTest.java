package cn.edu.nfu.topicselection.service;

import cn.edu.nfu.topicselection.constant.UserConstant;
import cn.edu.nfu.topicselection.manager.security.SecurityRateLimitManager;
import cn.edu.nfu.topicselection.service.impl.PasswordServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * 密码服务测试
 *
 * @author wobushi041
 */
class PasswordServiceTest {

    /**
     * 待测试密码服务
     */
    private final PasswordService passwordService = new PasswordServiceImpl(
            mock(UserService.class),
            mock(VerificationCodeService.class),
            mock(SecurityRateLimitManager.class),
            mock(ApplicationEventPublisher.class)
    );

    // 场景：测试新密码使用带随机盐的 BCrypt 摘要
    @Test
    void encodesNewPasswordsWithSaltedBcrypt() {
        // 1. 准备原始密码
        String rawPassword = "Correct-Horse-7!";

        // 2. 对同一密码执行两次编码
        String firstHash = passwordService.encodePassword(rawPassword);
        String secondHash = passwordService.encodePassword(rawPassword);

        // 3. 断言摘要格式、随机盐和密码匹配结果正确
        assertTrue(firstHash.startsWith("$2"));
        assertNotEquals(firstHash, secondHash);
        assertTrue(passwordService.matchesPassword(rawPassword, firstHash));
        assertFalse(passwordService.matchesPassword("wrong-password", firstHash));
        assertFalse(passwordService.needsPasswordUpgrade(firstHash));
    }

    // 场景：测试旧 MD5 密码仅用于兼容迁移校验
    @Test
    void acceptsLegacyMd5OnlyForMigration() {
        // 1. 准备旧版加盐 MD5 密码摘要
        String rawPassword = "legacy-password";
        String legacyHash = DigestUtils.md5DigestAsHex(
                (UserConstant.LEGACY_PASSWORD_SALT + rawPassword).getBytes(StandardCharsets.UTF_8));

        // 2. 调用密码升级判断和匹配方法

        // 3. 断言旧摘要需要升级且仅匹配正确密码
        assertTrue(passwordService.needsPasswordUpgrade(legacyHash));
        assertTrue(passwordService.matchesPassword(rawPassword, legacyHash));
        assertFalse(passwordService.matchesPassword("wrong-password", legacyHash));
    }

    // 场景：测试临时密码满足复杂度并保持随机唯一
    @Test
    void generatesUniqueStrongTemporaryPasswords() {
        // 1. 准备临时密码去重集合
        Set<String> passwords = new HashSet<>();

        // 2. 连续生成临时密码
        for (int i = 0; i < 100; i++) {
            String password = passwordService.generateTemporaryPassword();

            // 3. 断言每个密码的长度、复杂度和唯一性
            assertTrue(passwordService.isPasswordValid(password));
            assertTrue(password.matches(".*[A-Z].*"));
            assertTrue(password.matches(".*[a-z].*"));
            assertTrue(password.matches(".*[0-9].*"));
            assertTrue(password.matches(".*[!@#$%*\\-_].*"));
            assertTrue(passwords.add(password));
        }
    }

    // 场景：测试密码长度按照 UTF-8 字节数限制
    @Test
    void enforcesBcryptInputLengthInUtf8Bytes() {
        // 1. 准备不同字节长度的密码输入

        // 2. 调用密码长度校验方法

        // 3. 断言边界值和多字节字符按 UTF-8 字节数判断
        assertFalse(passwordService.isPasswordValid("1234567"));
        assertTrue(passwordService.isPasswordValid(repeat('a', 72)));
        assertFalse(passwordService.isPasswordValid(repeat('a', 73)));
        assertFalse(passwordService.isPasswordValid(repeat('密', 25)));
    }

    /**
     * 重复指定字符生成固定长度字符串
     *
     * @param character 重复字符
     * @param count     重复次数
     * @return 固定长度字符串
     */
    private static String repeat(char character, int count) {
        StringBuilder value = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            value.append(character);
        }
        return value.toString();
    }

}
