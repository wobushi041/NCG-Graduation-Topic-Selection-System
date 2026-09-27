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

/**
 * 认证控制器 HTTP 契约测试
 *
 * @author wobushi041
 */
class AuthControllerContractTest {

    /**
     * 认证模块预期 HTTP 路径集合
     */
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

    // 场景：测试认证控制器仅暴露 10 个受 Sentinel 保护的新接口
    @Test
    void exposesExactlyTheTenAuthEndpointsWithSentinelProtection() {
        // 1. 准备认证控制器根路径和实际路径集合
        RequestMapping root = AuthController.class.getAnnotation(RequestMapping.class);
        assertNotNull(root);
        assertEquals("/auth", root.value()[0]);

        Set<String> actualPaths = new LinkedHashSet<>();

        // 2. 收集认证接口并检查 Sentinel 限流注解
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

        // 3. 断言接口集合与预期契约一致
        assertEquals(EXPECTED_PATHS, actualPaths);
    }

    // 场景：测试认证控制器使用 Sa-Token 与自定义 DTO 校验注解
    @Test
    void usesSaTokenAnnotationsWithoutControllerValidAnnotations() {
        // 1. 检查公开接口与受保护接口的 Sa-Token 注解
        assertNotNull(method("login").getAnnotation(SaIgnore.class));
        assertNotNull(method("logout").getAnnotation(SaCheckLogin.class));
        assertNotNull(method("switchRole").getAnnotation(SaCheckRole.class));
        assertNotNull(method("adminReset").getAnnotation(SaCheckRole.class));
        assertNull(AuthController.class.getAnnotation(Validated.class));

        // 2. 检查请求参数未使用标准 Valid 注解
        for (Method method : AuthController.class.getDeclaredMethods()) {
            for (Parameter parameter : method.getParameters()) {
                assertFalse(parameter.isAnnotationPresent(Valid.class), method.getName());
            }

            // 3. 断言请求体方法均使用自定义 DTO 校验注解
            if (Arrays.stream(method.getParameterAnnotations())
                    .flatMap(Arrays::stream)
                    .map(Annotation::annotationType)
                    .anyMatch(type -> type.getSimpleName().equals("RequestBody"))) {
                assertNotNull(method.getAnnotation(ValidateRequest.class), method.getName());
            }
        }
    }

    // 场景：测试旧用户控制器不再暴露认证接口路径
    @Test
    void legacyUserControllerNoLongerDeclaresAuthRoutes() {
        // 1. 准备已经废弃的认证路径集合
        Set<String> legacyPaths = new LinkedHashSet<>(Arrays.asList(
                "/login", "/logout", "/toggle/login", "/toggle/available", "/reset/password",
                "/updata/password", "/send/code", "/send/captcha", "/check/captcha"
        ));

        // 2. 扫描旧用户控制器的请求映射
        for (Method method : UserController.class.getDeclaredMethods()) {
            PostMapping postMapping = method.getAnnotation(PostMapping.class);
            GetMapping getMapping = method.getAnnotation(GetMapping.class);

            // 3. 断言请求映射不包含废弃认证路径
            if (postMapping != null) {
                assertFalse(Arrays.stream(postMapping.value()).anyMatch(legacyPaths::contains), method.getName());
            }
            if (getMapping != null) {
                assertFalse(Arrays.stream(getMapping.value()).anyMatch(legacyPaths::contains), method.getName());
            }
        }
    }

    /**
     * 根据方法名称查找认证控制器方法
     *
     * @param name 方法名称
     * @return 认证控制器方法
     */
    private static Method method(String name) {
        return Arrays.stream(AuthController.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(name))
                .findFirst()
                .orElseThrow(AssertionError::new);
    }

}
