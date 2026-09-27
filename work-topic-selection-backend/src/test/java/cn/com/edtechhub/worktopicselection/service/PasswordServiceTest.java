package cn.com.edtechhub.worktopicselection.service;

import cn.com.edtechhub.worktopicselection.constant.UserConstant;
import cn.com.edtechhub.worktopicselection.manager.security.SecurityRateLimitManager;
import cn.com.edtechhub.worktopicselection.service.impl.PasswordServiceImpl;
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

class PasswordServiceTest {

    private final PasswordService passwordService = new PasswordServiceImpl(
            mock(UserService.class),
            mock(VerificationCodeService.class),
            mock(SecurityRateLimitManager.class),
            mock(ApplicationEventPublisher.class)
    );

    @Test
    void encodesNewPasswordsWithSaltedBcrypt() {
        String rawPassword = "Correct-Horse-7!";
        String firstHash = passwordService.encodePassword(rawPassword);
        String secondHash = passwordService.encodePassword(rawPassword);
        assertTrue(firstHash.startsWith("$2"));
        assertNotEquals(firstHash, secondHash);
        assertTrue(passwordService.matchesPassword(rawPassword, firstHash));
        assertFalse(passwordService.matchesPassword("wrong-password", firstHash));
        assertFalse(passwordService.needsPasswordUpgrade(firstHash));
    }

    @Test
    void acceptsLegacyMd5OnlyForMigration() {
        String rawPassword = "legacy-password";
        String legacyHash = DigestUtils.md5DigestAsHex(
                (UserConstant.LEGACY_PASSWORD_SALT + rawPassword).getBytes(StandardCharsets.UTF_8));
        assertTrue(passwordService.needsPasswordUpgrade(legacyHash));
        assertTrue(passwordService.matchesPassword(rawPassword, legacyHash));
        assertFalse(passwordService.matchesPassword("wrong-password", legacyHash));
    }

    @Test
    void generatesUniqueStrongTemporaryPasswords() {
        Set<String> passwords = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            String password = passwordService.generateTemporaryPassword();
            assertTrue(passwordService.isPasswordValid(password));
            assertTrue(password.matches(".*[A-Z].*"));
            assertTrue(password.matches(".*[a-z].*"));
            assertTrue(password.matches(".*[0-9].*"));
            assertTrue(password.matches(".*[!@#$%*\\-_].*"));
            assertTrue(passwords.add(password));
        }
    }

    @Test
    void enforcesBcryptInputLengthInUtf8Bytes() {
        assertFalse(passwordService.isPasswordValid("1234567"));
        assertTrue(passwordService.isPasswordValid(repeat('a', 72)));
        assertFalse(passwordService.isPasswordValid(repeat('a', 73)));
        assertFalse(passwordService.isPasswordValid(repeat('密', 25)));
    }

    private static String repeat(char character, int count) {
        StringBuilder value = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            value.append(character);
        }
        return value.toString();
    }
}
