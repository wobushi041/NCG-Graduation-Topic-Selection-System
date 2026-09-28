package cn.edu.nfu.topicselection.integration.auth;

import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import cn.edu.nfu.topicselection.integration.support.IntegrationAssertions;
import cn.edu.nfu.topicselection.integration.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

/**
 * 认证登录、Sa-Token Cookie 会话、退出与权限拦截集成测试
 *
 * @author wobushi041
 */
class AuthenticationFlowIT extends IntegrationTestBase {

    // 场景：测试登录下发 nfu-topic-selection Cookie 并验证访问受保护接口、退出登录及角色越权拦截
    @Test
    void authenticationLifecycle_shouldIssueCookieAllowLoginAccessRevokeOnLogoutAndRejectUnauthorizedRole() {
        // 1. 准备测试学生账号并发起登录请求
        testFixtureFactory.createDeptAndProject("计算机科学与工程系", "软件工程", "软件工程组");
        testFixtureFactory.createUser("20220001", "Pass@123456", "测试学生", 0, "计算机科学与工程系", "软件工程", 0);

        ResponseEntity<String> loginResp = testApiClient.loginRaw("20220001", "Pass@123456");
        IntegrationAssertions.assertBusinessCode(testApiClient, loginResp, CodeBindMessageEnums.SUCCESS.getCode());

        List<String> setCookies = loginResp.getHeaders().get(HttpHeaders.SET_COOKIE);
        Assertions.assertNotNull(setCookies);
        Assertions.assertTrue(setCookies.stream().anyMatch(cookie -> cookie.startsWith("nfu-topic-selection=")));

        String cookie = testApiClient.loginAndExtractCookie("20220001", "Pass@123456");

        // 2. 携带 Cookie 访问当前登录用户信息接口与管理员专属接口
        ResponseEntity<String> currentResp = testApiClient.get("/user/get/login", cookie);
        JsonNode currentJson = IntegrationAssertions.assertBusinessCode(
                testApiClient, currentResp, CodeBindMessageEnums.SUCCESS.getCode()
        );
        Assertions.assertEquals("20220001", currentJson.path("data").path("userAccount").asText());

        ResponseEntity<String> roleDeniedResp = testApiClient.postJson(
                "/user/add/dept", Collections.singletonMap("deptName", "越权系部"), cookie
        );
        IntegrationAssertions.assertBusinessCode(
                testApiClient, roleDeniedResp, CodeBindMessageEnums.NO_ROLE_ERROR.getCode()
        );

        // 3. 退出登录后再次携带旧 Cookie 访问受保护接口应返回未登录错误码
        ResponseEntity<String> logoutResp = testApiClient.postJson("/auth/logout", Collections.emptyMap(), cookie);
        IntegrationAssertions.assertBusinessCode(testApiClient, logoutResp, CodeBindMessageEnums.SUCCESS.getCode());

        ResponseEntity<String> afterLogoutResp = testApiClient.get("/user/get/login", cookie);
        IntegrationAssertions.assertBusinessCode(
                testApiClient, afterLogoutResp, CodeBindMessageEnums.NO_LOGIN_ERROR.getCode()
        );
    }

}
