package cn.com.edtechhub.worktopicselection.controller;

import cn.com.edtechhub.worktopicselection.annotation.SentinelRateLimit;
import cn.com.edtechhub.worktopicselection.annotation.ValidateRequest;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import org.junit.jupiter.api.Test;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuthControllerContractTest {

    private static final Set<String> EXPECTED_PATHS = new LinkedHashSet<>(Arrays.asList(
            "POST /auth/login",
            "POST /auth/logout",
            "POST /auth/role-switch",
            "GET /auth/role-switch/availability",
            "POST /auth/password/admin-reset",
            "POST /auth/password/change",
            "POST /auth/password/reset",
            "POST /auth/password/reset-code/send",
            "POST /auth/email-verification/code/send",
            "POST /auth/email-verification/code/verify"
    ));

    @Test
    void exposesExactlyTheTenAuthEndpointsWithSentinelProtection() {
        RequestMapping root = AuthController.class.getAnnotation(RequestMapping.class);
        assertNotNull(root);
        assertEquals("/auth", root.value()[0]);

        Set<String> actualPaths = new LinkedHashSet<>();
        for (Method method : AuthController.class.getDeclaredMethods()) {
            PostMapping postMapping = method.getAnnotation(PostMapping.class);
            GetMapping getMapping = method.getAnnotation(GetMapping.class);
            if (postMapping == null && getMapping == null) {
                continue;
            }
            String verb = postMapping == null ? "GET" : "POST";
            String path = postMapping == null ? getMapping.value()[0] : postMapping.value()[0];
            actualPaths.add(verb + " /auth" + path);
            assertNotNull(method.getAnnotation(SentinelRateLimit.class), method.getName());
        }
        assertEquals(EXPECTED_PATHS, actualPaths);
    }

    @Test
    void usesSaTokenAnnotationsWithoutControllerValidAnnotations() {
        assertNotNull(method("login").getAnnotation(SaIgnore.class));
        assertNotNull(method("logout").getAnnotation(SaCheckLogin.class));
        assertNotNull(method("switchRole").getAnnotation(SaCheckRole.class));
        assertNotNull(method("adminReset").getAnnotation(SaCheckRole.class));
        assertNull(AuthController.class.getAnnotation(Validated.class));

        for (Method method : AuthController.class.getDeclaredMethods()) {
            for (Parameter parameter : method.getParameters()) {
                assertFalse(parameter.isAnnotationPresent(Valid.class), method.getName());
            }
            if (Arrays.stream(method.getParameterAnnotations())
                    .flatMap(Arrays::stream)
                    .map(Annotation::annotationType)
                    .anyMatch(type -> type.getSimpleName().equals("RequestBody"))) {
                assertNotNull(method.getAnnotation(ValidateRequest.class), method.getName());
            }
        }
    }

    @Test
    void legacyUserControllerNoLongerDeclaresAuthRoutes() {
        Set<String> legacyPaths = new LinkedHashSet<>(Arrays.asList(
                "/login", "/logout", "/toggle/login", "/toggle/available", "/reset/password",
                "/updata/password", "/send/code", "/send/captcha", "/check/captcha"
        ));
        for (Method method : UserController.class.getDeclaredMethods()) {
            PostMapping postMapping = method.getAnnotation(PostMapping.class);
            GetMapping getMapping = method.getAnnotation(GetMapping.class);
            if (postMapping != null) {
                assertFalse(Arrays.stream(postMapping.value()).anyMatch(legacyPaths::contains), method.getName());
            }
            if (getMapping != null) {
                assertFalse(Arrays.stream(getMapping.value()).anyMatch(legacyPaths::contains), method.getName());
            }
        }
    }

    private static Method method(String name) {
        return Arrays.stream(AuthController.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(name))
                .findFirst()
                .orElseThrow(AssertionError::new);
    }
}
