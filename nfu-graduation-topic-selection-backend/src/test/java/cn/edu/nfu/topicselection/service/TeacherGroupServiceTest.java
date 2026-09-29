package cn.edu.nfu.topicselection.service;

import cn.edu.nfu.topicselection.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 教师分组配额服务单元测试
 *
 * @author wobushi041
 */
class TeacherGroupServiceTest {

    // 场景：测试各分组配额相互独立且已逻辑删除的题目不占用配额
    @Test
    void quotasAreIndependentAndDeletedTopicsDoNotConsumeCapacity() {
        JdbcTemplate db = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:quota;MODE=MySQL", "sa", ""));
        // 保持该数据库连接在隔离内存库生命周期内开启
        try (java.sql.Connection connection = db.getDataSource().getConnection()) {
            db.execute("CREATE TABLE topic_group(id BIGINT,groupName VARCHAR,isDelete INT)");
            db.execute("CREATE TABLE teacher_group_quota(teacherAccount VARCHAR,topicGroupId BIGINT,maxTopics INT)");
            db.execute("CREATE TABLE topic(id BIGINT,teacherAccount VARCHAR,topicGroupId BIGINT,isDelete INT)");
            db.update("INSERT INTO topic_group VALUES (1,'第一组',0),(3,'第三组',0)");
            db.update("INSERT INTO teacher_group_quota VALUES ('T1',1,3),('T1',3,5)");
            db.update("INSERT INTO topic VALUES (1,'T1',1,0),(2,'T1',1,0),(3,'T1',1,0),(4,'T1',3,1)");
            TeacherGroupService service = new TeacherGroupService();
            ReflectionTestUtils.setField(service,"jdbcTemplate",db);
            assertThrows(BusinessException.class, () -> service.validate("T1",1L,null));
            assertDoesNotThrow(() -> service.validate("T1",3L,null));
            assertDoesNotThrow(() -> service.validate("T1",1L,1L));
            assertThrows(BusinessException.class, () -> service.validate("T1",2L,null));
            assertThrows(BusinessException.class, () -> service.validate("T1",null,null));
            assertThrows(BusinessException.class, () -> service.validate("T2",1L,null));
        } catch (java.sql.SQLException e) {
            throw new AssertionError(e);
        }
    }

    // 场景：测试更新组选题额度成功且不能低于当前已出题目数量
    @Test
    void updateQuotaShouldPersistValidLimitAndRejectLimitBelowUsedCount() {
        // 1. 准备包含一条额度和两条有效题目的测试数据库
        JdbcTemplate db = new JdbcTemplate(
                new DriverManagerDataSource("jdbc:h2:mem:quota-update;MODE=MySQL", "sa", ""));
        try (java.sql.Connection connection = db.getDataSource().getConnection()) {
            db.execute("CREATE TABLE teacher_group_quota(teacherAccount VARCHAR,topicGroupId BIGINT,maxTopics INT)");
            db.execute("CREATE TABLE topic(id BIGINT,teacherAccount VARCHAR,topicGroupId BIGINT,isDelete INT)");
            db.update("INSERT INTO teacher_group_quota VALUES ('T1',1,3)");
            db.update("INSERT INTO topic VALUES (1,'T1',1,0),(2,'T1',1,0)");
            TeacherGroupService service = new TeacherGroupService();
            ReflectionTestUtils.setField(service, "jdbcTemplate", db);

            // 2. 更新有效额度并尝试设置低于已用数量的额度
            service.updateQuota("T1", 1L, 10);
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.updateQuota("T1", 1L, 1));

            // 3. 断言有效额度已持久化且非法额度被拒绝
            assertEquals(10, db.queryForObject(
                    "SELECT maxTopics FROM teacher_group_quota WHERE teacherAccount='T1' AND topicGroupId=1",
                    Integer.class));
            assertEquals("最大出题数量不能小于当前已出题目数量(2)", exception.getMessage());
        } catch (java.sql.SQLException e) {
            throw new AssertionError(e);
        }
    }

}
