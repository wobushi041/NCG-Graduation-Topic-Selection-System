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
            db.execute("CREATE TABLE teacher_group_quota(teacherAccount VARCHAR,groupName VARCHAR,maxTopics INT)");
            db.execute("CREATE TABLE topic(id BIGINT,teacherAccount VARCHAR,topicGroup VARCHAR,isDelete INT)");
            db.update("INSERT INTO teacher_group_quota VALUES ('T1','第一组',3),('T1','第三组',5)");
            db.update("INSERT INTO topic VALUES (1,'T1','第一组',0),(2,'T1','第一组',0),(3,'T1','第一组',0),(4,'T1','第三组',1)");
            TeacherGroupService service = new TeacherGroupService();
            ReflectionTestUtils.setField(service,"jdbcTemplate",db);
            assertThrows(BusinessException.class, () -> service.validate("T1","第一组",null));
            assertDoesNotThrow(() -> service.validate("T1","第三组",null));
            assertDoesNotThrow(() -> service.validate("T1","第一组",1L));
            assertThrows(BusinessException.class, () -> service.validate("T1","第二组",null));
            assertThrows(BusinessException.class, () -> service.validate("T1",null,null));
            assertThrows(BusinessException.class, () -> service.validate("T2","第一组",null));
        } catch (java.sql.SQLException e) {
            throw new AssertionError(e);
        }
    }

}
