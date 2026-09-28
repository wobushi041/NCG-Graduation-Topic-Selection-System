package cn.edu.nfu.topicselection.integration.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;

/**
 * 集成测试 HTTP 客户端辅助组件，封装 Cookie 会话携带与 JSON 响应解析
 *
 * @author wobushi041
 */
@Component
public class TestApiClient {

    /**
     * 注入 TestRestTemplate 依赖
     */
    @Resource
    private TestRestTemplate restTemplate;

    /**
     * 注入 Jackson ObjectMapper 依赖
     */
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 当前随机端口
     */
    private int serverPort;

    /**
     * 设置当前随机服务端口
     *
     * @param serverPort 本地随机端口
     */
    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    /**
     * 拼接完整本地请求 URL
     *
     * @param path 接口相对路径
     * @return 完整 HTTP URL
     */
    public String buildUrl(String path) {
        return "http://127.0.0.1:" + serverPort + path;
    }

    /**
     * 调用登录接口并返回原始 HTTP 响应实体
     *
     * @param userAccount  登录账号
     * @param userPassword 登录密码
     * @return 字符串响应实体
     */
    public ResponseEntity<String> loginRaw(String userAccount, String userPassword) {
        Map<String, Object> body = new HashMap<>();
        body.put("userAccount", userAccount);
        body.put("userPassword", userPassword);
        return postJson("/auth/login", body, null);
    }

    /**
     * 登录并提取响应头中的 Cookie 键值对字符串
     *
     * @param userAccount  登录账号
     * @param userPassword 登录密码
     * @return Cookie 请求头字符串
     */
    public String loginAndExtractCookie(String userAccount, String userPassword) {
        ResponseEntity<String> response = loginRaw(userAccount, userPassword);
        List<String> setCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (setCookies == null || setCookies.isEmpty()) {
            return "";
        }
        String firstCookie = setCookies.get(0);
        int semiIndex = firstCookie.indexOf(';');
        return semiIndex > 0 ? firstCookie.substring(0, semiIndex) : firstCookie;
    }

    /**
     * 发送带可选 Cookie 的 JSON POST 请求
     *
     * @param path   请求路径
     * @param body   请求体对象
     * @param cookie 可选 Cookie 字符串
     * @return 字符串响应实体
     */
    public ResponseEntity<String> postJson(String path, Object body, String cookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (cookie != null && !cookie.isEmpty()) {
            headers.add(HttpHeaders.COOKIE, cookie);
        }
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(buildUrl(path), HttpMethod.POST, entity, String.class);
    }

    /**
     * 发送带可选 Cookie 的 GET 请求
     *
     * @param path   请求路径
     * @param cookie 可选 Cookie 字符串
     * @return 字符串响应实体
     */
    public ResponseEntity<String> get(String path, String cookie) {
        HttpHeaders headers = new HttpHeaders();
        if (cookie != null && !cookie.isEmpty()) {
            headers.add(HttpHeaders.COOKIE, cookie);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(buildUrl(path), HttpMethod.GET, entity, String.class);
    }

    /**
     * 将 JSON 响应字符串解析为 JsonNode
     *
     * @param json JSON 字符串
     * @return 解析后的 JsonNode
     */
    public JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException("解析响应 JSON 失败", e);
        }
    }

}
