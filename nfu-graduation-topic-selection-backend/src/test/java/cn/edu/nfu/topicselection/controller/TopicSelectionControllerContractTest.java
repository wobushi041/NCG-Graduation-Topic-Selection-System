package cn.edu.nfu.topicselection.controller;

import cn.edu.nfu.topicselection.annotation.SentinelRateLimit;
import cn.edu.nfu.topicselection.annotation.ValidateRequest;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
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
 * 学生选题与教师确认控制器 HTTP 契约测试
 *
 * @author wobushi041
 */
class TopicSelectionControllerContractTest {

    /**
     * 选题模块预期 HTTP 路径集合（4 个写用例 + 5 个读用例）
     */
    private static final Set<String> EXPECTED_PATHS = new LinkedHashSet<>(Arrays.asList(
            "POST /user/preselect/topic/by/id",
            "POST /user/select/topic/by/id",
            "POST /user/select/student",
            "POST /user/withdraw",
            "POST /user/get/select/topic/by/id",
            "POST /user/get/preselect/topic",
            "POST /user/get/select/topic",
            "POST /user/get/select/topic/choice_time",
            "POST /user/get/student/by/topicId"
    ));

    // 场景：测试选题控制器仅暴露 9 个受 Sentinel 保护的选题读写接口
    @Test
    void exposesExactlyTheNineTopicSelectionEndpointsWithSentinelProtection() {
        // 1. 准备选题控制器根路径和实际路径集合
        RequestMapping root = TopicSelectionController.class.getAnnotation(RequestMapping.class);
        assertNotNull(root);
        assertEquals("/user", root.value()[0]);

        Set<String> actualPaths = new LinkedHashSet<>();

        // 2. 收集选题读写接口并检查 Sentinel 限流注解
        for (Method method : TopicSelectionController.class.getDeclaredMethods()) {
            PostMapping postMapping = method.getAnnotation(PostMapping.class);
            GetMapping getMapping = method.getAnnotation(GetMapping.class);
            if (postMapping == null && getMapping == null) {
                continue;
            }
            String verb = postMapping == null ? "GET" : "POST";
            String path = postMapping == null ? getMapping.value()[0] : postMapping.value()[0];
            actualPaths.add(verb + " /user" + path);
            assertNotNull(method.getAnnotation(SentinelRateLimit.class), method.getName());
        }

        // 3. 断言接口集合与预期契约一致
        assertEquals(EXPECTED_PATHS, actualPaths);
    }

    // 场景：测试选题控制器使用 Sa-Token 与自定义 DTO 校验注解
    @Test
    void usesSaTokenAnnotationsWithoutControllerValidAnnotations() {
        // 1. 检查受保护接口的 Sa-Token 登录与角色注解
        for (String methodName : Arrays.asList(
                "preSelectTopicById", "selectTopicById", "selectStudent", "withdraw",
                "getSelectTopicById", "getPreTopic", "getSelectTopic", "getSelectTopicTime", "getStudentByTopicId"
        )) {
            assertNotNull(method(methodName).getAnnotation(SaCheckLogin.class), methodName);
            assertNotNull(method(methodName).getAnnotation(SaCheckRole.class), methodName);
        }
        assertNull(TopicSelectionController.class.getAnnotation(Validated.class));

        // 2. 检查请求参数未使用标准 Valid 注解
        for (Method method : TopicSelectionController.class.getDeclaredMethods()) {
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

    // 场景：测试旧用户控制器不再声明已迁出的全部 9 个选题接口路径
    @Test
    void legacyUserControllerNoLongerDeclaresSelectionRoutes() {
        // 1. 准备已迁出的 9 个选题读写接口路径集合
        Set<String> migratedPaths = new LinkedHashSet<>(Arrays.asList(
                "/preselect/topic/by/id",
                "/select/topic/by/id",
                "/select/student",
                "/withdraw",
                "/get/select/topic/by/id",
                "/get/preselect/topic",
                "/get/select/topic",
                "get/select/topic",
                "/get/select/topic/choice_time",
                "/get/student/by/topicId"
        ));

        // 2. 扫描旧用户控制器的请求映射
        for (Method method : UserController.class.getDeclaredMethods()) {
            PostMapping postMapping = method.getAnnotation(PostMapping.class);
            GetMapping getMapping = method.getAnnotation(GetMapping.class);

            // 3. 断言请求映射不包含已迁出的选题路径
            if (postMapping != null) {
                assertFalse(Arrays.stream(postMapping.value()).anyMatch(migratedPaths::contains), method.getName());
            }
            if (getMapping != null) {
                assertFalse(Arrays.stream(getMapping.value()).anyMatch(migratedPaths::contains), method.getName());
            }
        }
    }

    /**
     * 根据方法名称查找选题控制器方法
     *
     * @param name 方法名称
     * @return 反射方法对象
     */
    private static Method method(String name) {
        return Arrays.stream(TopicSelectionController.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing method: " + name));
    }

}
