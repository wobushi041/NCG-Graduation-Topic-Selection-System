package cn.edu.nfu.topicselection.integration.selection;

import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import cn.edu.nfu.topicselection.integration.support.IntegrationAssertions;
import cn.edu.nfu.topicselection.integration.support.IntegrationTestBase;
import cn.edu.nfu.topicselection.mapper.TopicMapper;
import cn.edu.nfu.topicselection.model.entity.Topic;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Resource;

/**
 * 学生预选、最终确认、选题查询与退选恢复余量完整链路集成测试
 *
 * @author wobushi041
 */
class TopicSelectionFlowIT extends IntegrationTestBase {

    /**
     * 注入课题 Mapper 依赖
     */
    @Resource
    private TopicMapper topicMapper;

    // 场景：测试学生预选、提交最终选题、查询已选课题及退选后恢复课题余量
    @Test
    void topicSelectionFlow_shouldCompletePreselectConfirmQueryAndWithdraw() {
        // 1. 准备系部、专业、指导教师、学生及已发布课题
        testFixtureFactory.createDeptAndProject("计算机系", "计算机科学与技术", "计科组");
        testFixtureFactory.createUser("T2001", "Pass@123456", "陈老师", 1, "计算机系", "计算机科学与技术", 5);
        testFixtureFactory.createUser("S2001", "Pass@123456", "赵同学", 0, "计算机系", "计算机科学与技术", 0);
        Topic topic = testFixtureFactory.createPublishedTopic(
                "分布式缓存一致性研究与实现", "T2001", "陈老师", "计算机系", "计科组", 1
        );

        String studentCookie = testApiClient.loginAndExtractCookie("S2001", "Pass@123456");

        // 2. 执行预选与最终确认选题并校验课题剩余容量扣减为 0
        Map<String, Object> preselectReq = new HashMap<>();
        preselectReq.put("id", topic.getId());
        preselectReq.put("status", 0);
        ResponseEntity<String> preResp = testApiClient.postJson(
                "/user/preselect/topic/by/id", preselectReq, studentCookie
        );
        IntegrationAssertions.assertBusinessCode(testApiClient, preResp, CodeBindMessageEnums.SUCCESS.getCode());

        Map<String, Object> confirmReq = new HashMap<>();
        confirmReq.put("id", topic.getId());
        ResponseEntity<String> confirmResp = testApiClient.postJson(
                "/user/select/topic/by/id", confirmReq, studentCookie
        );
        IntegrationAssertions.assertBusinessCode(testApiClient, confirmResp, CodeBindMessageEnums.SUCCESS.getCode());

        Topic afterConfirm = topicMapper.selectById(topic.getId());
        Assertions.assertEquals(0, afterConfirm.getSurplusQuantity().intValue());

        ResponseEntity<String> queryResp = testApiClient.postJson(
                "/user/get/select/topic", Collections.emptyMap(), studentCookie
        );
        JsonNode queryRoot = IntegrationAssertions.assertBusinessCode(
                testApiClient, queryResp, CodeBindMessageEnums.SUCCESS.getCode()
        );
        Assertions.assertEquals(1, queryRoot.path("data").size());

        // 3. 学生发起退选并断言数据库课题余量恢复为 1
        Map<String, Object> withdrawReq = new HashMap<>();
        withdrawReq.put("topicId", topic.getId());
        ResponseEntity<String> withdrawResp = testApiClient.postJson("/user/withdraw", withdrawReq, studentCookie);
        IntegrationAssertions.assertBusinessCode(testApiClient, withdrawResp, CodeBindMessageEnums.SUCCESS.getCode());

        Topic afterWithdraw = topicMapper.selectById(topic.getId());
        Assertions.assertEquals(1, afterWithdraw.getSurplusQuantity().intValue());
    }

}
