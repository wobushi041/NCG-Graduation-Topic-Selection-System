package cn.edu.nfu.topicselection.integration.support;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import javax.annotation.Resource;

/**
 * 集成测试数据库表与 Redis 键清理组件
 *
 * @author wobushi041
 */
@Component
public class DatabaseCleaner {

    /**
     * 注入 JdbcTemplate 依赖
     */
    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 注入 StringRedisTemplate 依赖
     */
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 清空业务表数据与 Redis 键，恢复默认系统开关状态
     */
    public void cleanAll() {
        jdbcTemplate.execute("DELETE FROM `student_topic_selection`");
        jdbcTemplate.execute("DELETE FROM `topic`");
        jdbcTemplate.execute("DELETE FROM `project`");
        jdbcTemplate.execute("DELETE FROM `dept`");
        jdbcTemplate.execute("DELETE FROM `user`");
        jdbcTemplate.execute("DELETE FROM `switch`");

        // 初始化基础系统开关默认值
        jdbcTemplate.update(
                "INSERT INTO `switch` (`name`, `status`) VALUES ('cross_topic', 0), ('view_topic', 1), ('switch_single_choice', 0), ('topic_lock', 0)"
        );

        Set<String> keys = stringRedisTemplate.keys("*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

}
