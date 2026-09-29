package cn.edu.nfu.topicselection.integration.documentation;

import cn.edu.nfu.topicselection.integration.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.annotation.Resource;

/**
 * Knife4j OpenAPI2 运行时接口文档与 Controller 映射集合契约集成测试
 *
 * @author wobushi041
 */
class Knife4jDocumentationIT extends IntegrationTestBase {

    /**
     * 注入 Spring MVC RequestMappingHandlerMapping 依赖
     */
    @Resource
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    // 场景：测试 Knife4j 文档页面、OpenAPI2 JSON 端点及 79 个业务 HTTP 操作集合与运行时 Controller 映射完全一致
    @Test
    void knife4jDocumentation_shouldExposeAll79ControllerOperationsAndMatchRuntimeMappings() {
        // 1. 请求 /doc.html 与 /v2/api-docs 并校验基础元数据
        ResponseEntity<String> docHtmlResp = testApiClient.get("/doc.html", null);
        Assertions.assertEquals(200, docHtmlResp.getStatusCodeValue());
        Assertions.assertNotNull(docHtmlResp.getBody());
        Assertions.assertFalse(docHtmlResp.getBody().trim().isEmpty());

        ResponseEntity<String> apiDocsResp = testApiClient.get("/v2/api-docs", null);
        Assertions.assertEquals(200, apiDocsResp.getStatusCodeValue());
        Assertions.assertNotNull(apiDocsResp.getBody());

        JsonNode root = testApiClient.parseJson(apiDocsResp.getBody());
        Assertions.assertEquals("2.0", root.path("swagger").asText());
        Assertions.assertEquals("接口文档", root.path("info").path("title").asText());

        // 2. 提取 /v2/api-docs 中的业务操作集合并校验 80 个操作、关键重构路由及废弃路由不存在
        Set<String> documentedOperations = extractDocumentedOperations(root.path("paths"));
        Assertions.assertEquals(80, documentedOperations.size());

        List<String> requiredOperations = Arrays.asList(
                "POST /auth/login",
                "POST /auth/logout",
                "POST /user/add/topic",
                "POST /user/teacher/group/quota",
                "POST /user/preselect/topic/by/id",
                "POST /user/select/topic/by/id",
                "POST /user/withdraw",
                "POST /file/upload",
                "POST /ai/send"
        );
        for (String op : requiredOperations) {
            Assertions.assertTrue(documentedOperations.contains(op), "缺少关键业务操作: " + op);
        }

        List<String> deprecatedOperations = Arrays.asList(
                "POST /user/login",
                "POST /user/logout",
                "POST /user/toggle/login",
                "POST /user/reset/password",
                "POST /user/send/code",
                "POST /user/send/captcha"
        );
        for (String deprecatedOp : deprecatedOperations) {
            Assertions.assertFalse(documentedOperations.contains(deprecatedOp), "不应包含已废弃路由: " + deprecatedOp);
        }

        // 3. 收集运行时 Controller 的方法与路径映射并与文档集合比对，同时校验 release Profile 关闭文档
        Set<String> runtimeOperations = collectMajorControllerOperations();
        Assertions.assertEquals(runtimeOperations, documentedOperations);

        YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
        yamlFactory.setResources(new ClassPathResource("application-release.yaml"));
        Properties releaseProps = yamlFactory.getObject();
        Assertions.assertNotNull(releaseProps);
        Assertions.assertEquals("false", String.valueOf(releaseProps.get("knife4j.enable")));
    }

    /**
     * 从 Swagger paths 节点提取所有 HTTP 方法与路径组合
     *
     * @param pathsNode Swagger paths JSON 节点
     * @return 形如 METHOD /path 的操作集合
     */
    private Set<String> extractDocumentedOperations(JsonNode pathsNode) {
        Set<String> operations = new HashSet<>();
        Iterator<Map.Entry<String, JsonNode>> pathFields = pathsNode.fields();
        while (pathFields.hasNext()) {
            Map.Entry<String, JsonNode> pathEntry = pathFields.next();
            String path = pathEntry.getKey();
            Iterator<String> methodNames = pathEntry.getValue().fieldNames();
            while (methodNames.hasNext()) {
                String method = methodNames.next().toUpperCase(Locale.ROOT);
                operations.add(method + " " + path);
            }
        }
        return operations;
    }

    /**
     * 从 RequestMappingHandlerMapping 收集本项目 Controller 包下的全部 HTTP 映射操作
     *
     * @return 形如 METHOD /path 的运行时操作集合
     */
    private Set<String> collectMajorControllerOperations() {
        Set<String> operations = new HashSet<>();
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMappingHandlerMapping.getHandlerMethods();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            Class<?> beanType = entry.getValue().getBeanType();
            if (!beanType.getPackage().getName().startsWith("cn.edu.nfu.topicselection.controller")) {
                continue;
            }
            Set<String> patterns = entry.getKey().getPatternsCondition().getPatterns();
            Set<RequestMethod> methods = entry.getKey().getMethodsCondition().getMethods();
            for (String pattern : patterns) {
                for (RequestMethod method : methods) {
                    operations.add(method.name() + " " + pattern);
                }
            }
        }
        return operations;
    }

}
