package cn.edu.nfu.topicselection.integration.selection;

import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import cn.edu.nfu.topicselection.integration.support.IntegrationAssertions;
import cn.edu.nfu.topicselection.integration.support.IntegrationTestBase;
import cn.edu.nfu.topicselection.mapper.StudentTopicSelectionMapper;
import cn.edu.nfu.topicselection.mapper.TopicMapper;
import cn.edu.nfu.topicselection.mapper.UserMapper;
import cn.edu.nfu.topicselection.model.entity.StudentTopicSelection;
import cn.edu.nfu.topicselection.model.entity.Topic;
import cn.edu.nfu.topicselection.model.entity.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import javax.annotation.Resource;

/**
 * 选题多步写操作中途异常触发数据库事务原子回滚集成测试
 *
 * @author wobushi041
 */
class TopicSelectionRollbackIT extends IntegrationTestBase {

    /**
     * 注入课题 Mapper 依赖
     */
    @Resource
    private TopicMapper topicMapper;

    /**
     * 注入用户 Mapper 依赖
     */
    @Resource
    private UserMapper userMapper;

    /**
     * 侦测学生选题关联 Mapper，用于制造受控持久层异常
     */
    @SpyBean
    private StudentTopicSelectionMapper studentTopicSelectionMapper;

    // 场景：测试提交最终选题过程中关联表写入抛出异常时课题余量与用户状态完整回滚
    @Test
    void selectTopicById_whenSelectionInsertFails_shouldRollbackTopicSurplusAndStudentState() {
        // 1. 准备教师、学生及剩余容量为 1 的已发布课题
        testFixtureFactory.createCollegeAndMajor("网络工程系", "网络工程", "网工组");
        testFixtureFactory.createUser("T4001", "Pass@123456", "吴老师", 1, "网络工程系", "网络工程", 5);
        User student = testFixtureFactory.createUser("S4001", "Pass@123456", "孙同学", 0, "网络工程系", "网络工程", 0);
        Topic topic = testFixtureFactory.createPublishedTopic(
                "事务回滚原子性保障验证课题", "T4001", "吴老师", "网络工程系", "网工组", 1
        );

        String studentCookie = testApiClient.loginAndExtractCookie("S4001", "Pass@123456");

        // 2. 配置 SpyBean 在插入选题关联记录时抛出受控异常并调用确认选题接口
        Mockito.doThrow(new RuntimeException("受控数据库写入故障"))
                .when(studentTopicSelectionMapper)
                .insert(Mockito.any(StudentTopicSelection.class));

        try {
            ResponseEntity<String> resp = testApiClient.postJson(
                    "/user/select/topic/by/id", Collections.singletonMap("id", topic.getId()), studentCookie
            );
            IntegrationAssertions.assertBusinessCode(
                    testApiClient, resp, CodeBindMessageEnums.SYSTEM_ERROR.getCode()
            );

            // 3. 断言课题余量仍为 1、学生已选数量仍为 0 且不存在部分提交的选题记录
            Topic reloadedTopic = topicMapper.selectById(topic.getId());
            Assertions.assertEquals(1, reloadedTopic.getSurplusQuantity().intValue());

            User reloadedStudent = userMapper.selectById(student.getId());
            Assertions.assertEquals(0, reloadedStudent.getTopicAmount().intValue());

            List<StudentTopicSelection> records = studentTopicSelectionMapper.selectList(
                    new LambdaQueryWrapper<StudentTopicSelection>()
                            .eq(StudentTopicSelection::getTopicId, topic.getId())
            );
            Assertions.assertTrue(records.isEmpty());
        } finally {
            Mockito.reset(studentTopicSelectionMapper);
        }
    }

}
