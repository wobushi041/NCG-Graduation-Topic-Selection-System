package cn.edu.nfu.topicselection.integration.support;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.springframework.http.ResponseEntity;

/**
 * 集成测试通用响应断言工具类
 *
 * @author wobushi041
 */
public final class IntegrationAssertions {

    /**
     * 私有构造方法，禁止实例化
     */
    private IntegrationAssertions() {
    }

    /**
     * 断言 HTTP 状态码为 200 且业务响应码等于预期值
     *
     * @param client       测试 API 客户端
     * @param response     HTTP 响应实体
     * @param expectedCode 预期业务响应码
     * @return 解析后的响应根节点
     */
    public static JsonNode assertBusinessCode(TestApiClient client, ResponseEntity<String> response, int expectedCode) {
        Assertions.assertEquals(200, response.getStatusCodeValue());
        Assertions.assertNotNull(response.getBody());
        JsonNode root = client.parseJson(response.getBody());
        Assertions.assertEquals(expectedCode, root.path("code").asInt(), "业务状态码不匹配: " + response.getBody());
        return root;
    }

}
