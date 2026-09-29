package cn.edu.nfu.topicselection.integration.selection;

import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import cn.edu.nfu.topicselection.integration.support.IntegrationTestBase;
import cn.edu.nfu.topicselection.mapper.StudentTopicSelectionMapper;
import cn.edu.nfu.topicselection.mapper.TopicMapper;
import cn.edu.nfu.topicselection.model.entity.StudentTopicSelection;
import cn.edu.nfu.topicselection.model.entity.Topic;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;

/**
 * 学生并发争抢同一课题最后一个名额的悲观锁集成测试
 *
 * @author wobushi041
 */
class TopicSelectionConcurrencyIT extends IntegrationTestBase {

    /**
     * 注入课题 Mapper 依赖
     */
    @Resource
    private TopicMapper topicMapper;

    /**
     * 注入学生选题关联 Mapper 依赖
     */
    @Resource
    private StudentTopicSelectionMapper studentTopicSelectionMapper;

    // 场景：测试两名学生并发抢选同一课题最后一个名额时仅一人成功且余量不为负
    @Test
    void concurrentConfirmSelection_givenSingleRemainingQuota_allowsExactlyOneStudent() throws Exception {
        // 1. 准备一名教师、两名学生及仅剩 1 个名额的已发布课题
        testFixtureFactory.createCollegeAndMajor("人工智能系", "智能科学与技术", "智能组");
        testFixtureFactory.createUser("T3001", "Pass@123456", "周老师", 1, "人工智能系", "智能科学与技术", 5);
        testFixtureFactory.createUser("S3001", "Pass@123456", "学生甲", 0, "人工智能系", "智能科学与技术", 0);
        testFixtureFactory.createUser("S3002", "Pass@123456", "学生乙", 0, "人工智能系", "智能科学与技术", 0);

        Topic topic = testFixtureFactory.createPublishedTopic(
                "高并发选课悲观行锁验证课题", "T3001", "周老师", "人工智能系", "智能组", 1
        );

        String cookieA = testApiClient.loginAndExtractCookie("S3001", "Pass@123456");
        String cookieB = testApiClient.loginAndExtractCookie("S3002", "Pass@123456");

        // 2. 使用栅栏同步两个并发线程同时提交最终选题请求
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> futureA = executor.submit(() -> {
                startGate.await(5, TimeUnit.SECONDS);
                ResponseEntity<String> resp = testApiClient.postJson(
                        "/user/select/topic/by/id", Collections.singletonMap("id", topic.getId()), cookieA
                );
                JsonNode root = testApiClient.parseJson(resp.getBody());
                return root.path("code").asInt();
            });
            Future<Integer> futureB = executor.submit(() -> {
                startGate.await(5, TimeUnit.SECONDS);
                ResponseEntity<String> resp = testApiClient.postJson(
                        "/user/select/topic/by/id", Collections.singletonMap("id", topic.getId()), cookieB
                );
                JsonNode root = testApiClient.parseJson(resp.getBody());
                return root.path("code").asInt();
            });

            startGate.countDown();
            int codeA = futureA.get(15, TimeUnit.SECONDS);
            int codeB = futureB.get(15, TimeUnit.SECONDS);

            // 3. 断言仅一人返回 SUCCESS，课题余量精确为 0，且确认选题记录数为 1
            int successCode = CodeBindMessageEnums.SUCCESS.getCode();
            boolean exactlyOneSucceeded = (codeA == successCode && codeB != successCode)
                    || (codeA != successCode && codeB == successCode);
            Assertions.assertTrue(exactlyOneSucceeded, "并发选题必须仅一人成功: codeA=" + codeA + ", codeB=" + codeB);

            Topic updatedTopic = topicMapper.selectById(topic.getId());
            Assertions.assertEquals(0, updatedTopic.getSurplusQuantity().intValue());

            List<StudentTopicSelection> confirmedSelections = studentTopicSelectionMapper.selectList(
                    new LambdaQueryWrapper<StudentTopicSelection>()
                            .eq(StudentTopicSelection::getTopicId, topic.getId())
                            .eq(StudentTopicSelection::getStatus, 2)
            );
            Assertions.assertEquals(1, confirmedSelections.size());
        } finally {
            executor.shutdownNow();
        }
    }

}
