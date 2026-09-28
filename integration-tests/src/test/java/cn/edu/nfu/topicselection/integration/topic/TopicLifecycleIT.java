package cn.edu.nfu.topicselection.integration.topic;

import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import cn.edu.nfu.topicselection.integration.support.IntegrationAssertions;
import cn.edu.nfu.topicselection.integration.support.IntegrationTestBase;
import cn.edu.nfu.topicselection.mapper.TopicMapper;
import cn.edu.nfu.topicselection.model.entity.Topic;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Resource;

/**
 * 课题出题、系主任审核、管理员发布与学生查询全生命周期集成测试
 *
 * @author wobushi041
 */
class TopicLifecycleIT extends IntegrationTestBase {

    /**
     * 注入课题 Mapper 依赖
     */
    @Resource
    private TopicMapper topicMapper;

    // 场景：测试教师出题、系部主任审核通过、管理员设置开放时间发布及学生分页查询可见已发布课题
    @Test
    void topicLifecycle_shouldFlowFromTeacherSubmissionThroughReviewAndPublishToStudentQuery() {
        // 1. 初始化系部、专业及教师、系主任、管理员、学生四个角色账号
        testFixtureFactory.createDeptAndProject("软件工程系", "软件工程", "软件工程组");
        testFixtureFactory.createUser("T1001", "Pass@123456", "张老师", 1, "软件工程系", "软件工程", 5);
        testFixtureFactory.createUser("D1001", "Pass@123456", "李主任", 2, "软件工程系", "软件工程", 5);
        testFixtureFactory.createUser("A1001", "Pass@123456", "系统管理员", 3, "软件工程系", "软件工程", 0);
        testFixtureFactory.createUser("S1001", "Pass@123456", "王同学", 0, "软件工程系", "软件工程", 0);

        String teacherCookie = testApiClient.loginAndExtractCookie("T1001", "Pass@123456");
        String deptCookie = testApiClient.loginAndExtractCookie("D1001", "Pass@123456");
        String adminCookie = testApiClient.loginAndExtractCookie("A1001", "Pass@123456");
        String studentCookie = testApiClient.loginAndExtractCookie("S1001", "Pass@123456");

        // 2. 教师新增课题，系部主任审核通过（status = 0），管理员发布并设置开放时间（status = 1）
        Map<String, Object> addReq = new HashMap<>();
        addReq.put("topic", "基于微服务的毕业选题系统设计与实现");
        addReq.put("type", "工程设计");
        addReq.put("description", "设计并实现一套支持高并发选课与多角色审核的毕业选题管理系统");
        addReq.put("requirement", "掌握 Java 与 Spring Boot 开发");
        addReq.put("deptName", "软件工程系");
        addReq.put("deptTeacher", "李主任");

        ResponseEntity<String> addResp = testApiClient.postJson("/user/add/topic", addReq, teacherCookie);
        IntegrationAssertions.assertBusinessCode(testApiClient, addResp, CodeBindMessageEnums.SUCCESS.getCode());

        Topic savedTopic = topicMapper.selectOne(
                new LambdaQueryWrapper<Topic>()
                        .eq(Topic::getTopic, "基于微服务的毕业选题系统设计与实现")
        );
        Assertions.assertNotNull(savedTopic);
        Assertions.assertEquals(-1, savedTopic.getStatus().intValue());

        Map<String, Object> checkReq = new HashMap<>();
        checkReq.put("id", savedTopic.getId());
        checkReq.put("status", 0);
        checkReq.put("reason", "审核通过");
        ResponseEntity<String> checkResp = testApiClient.postJson("/user/check/topic", checkReq, deptCookie);
        IntegrationAssertions.assertBusinessCode(testApiClient, checkResp, CodeBindMessageEnums.SUCCESS.getCode());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long now = System.currentTimeMillis();
        Map<String, Object> publishReq = new HashMap<>();
        publishReq.put("idList", Collections.singletonList(savedTopic.getId()));
        publishReq.put("startTime", sdf.format(new Date(now - 60_000L)));
        publishReq.put("endTime", sdf.format(new Date(now + 3600_000L)));
        ResponseEntity<String> pubResp = testApiClient.postJson("/user/set/time/by/id", publishReq, adminCookie);
        IntegrationAssertions.assertBusinessCode(testApiClient, pubResp, CodeBindMessageEnums.SUCCESS.getCode());

        // 3. 学生分页查询可见课题并断言能查询到已发布的课题
        Map<String, Object> queryReq = new HashMap<>();
        queryReq.put("current", 1);
        queryReq.put("pageSize", 10);
        ResponseEntity<String> queryResp = testApiClient.postJson("/user/get/topic/page", queryReq, studentCookie);
        JsonNode queryRoot = IntegrationAssertions.assertBusinessCode(
                testApiClient, queryResp, CodeBindMessageEnums.SUCCESS.getCode()
        );
        JsonNode records = queryRoot.path("data").path("records");
        Assertions.assertEquals(1, records.size());
        Assertions.assertEquals("基于微服务的毕业选题系统设计与实现", records.get(0).path("topic").asText());
    }

}
