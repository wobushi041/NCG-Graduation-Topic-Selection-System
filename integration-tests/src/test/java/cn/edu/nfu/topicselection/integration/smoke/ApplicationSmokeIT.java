package cn.edu.nfu.topicselection.integration.smoke;

import cn.edu.nfu.topicselection.constant.RedisConstant;
import cn.edu.nfu.topicselection.integration.support.IntegrationTestBase;
import cn.edu.nfu.topicselection.manager.redis.RedisManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.sql.Connection;
import java.util.Set;
import javax.annotation.Resource;
import javax.sql.DataSource;

/**
 * Spring 上下文与容器基础设施冒烟集成测试
 *
 * @author wobushi041
 */
class ApplicationSmokeIT extends IntegrationTestBase {

    /**
     * 注入 Spring Environment 依赖
     */
    @Resource
    private Environment environment;

    /**
     * 注入 DataSource 依赖
     */
    @Resource
    private DataSource dataSource;

    /**
     * 注入 RedisConnectionFactory 依赖
     */
    @Resource
    private RedisConnectionFactory redisConnectionFactory;

    /**
     * 注入 RedisManager 依赖
     */
    @Resource
    private RedisManager redisManager;

    /**
     * 注入 StringRedisTemplate 依赖
     */
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 场景：测试 Spring 上下文激活 integration Profile 且连接真实容器 MySQL 与 Redis
    @Test
    void contextLoads_shouldConnectToContainerMySqlAndRedisWithExpectedPrefixes() throws Exception {
        // 1. 校验当前激活的 Profile 仅包含 integration
        String[] activeProfiles = environment.getActiveProfiles();
        Assertions.assertArrayEquals(new String[]{"integration"}, activeProfiles);

        // 2. 校验 JDBC 实际连接元数据指向容器 MySQL 且当前数据库名为 nfu_topic_selection
        try (Connection connection = dataSource.getConnection()) {
            String jdbcUrl = connection.getMetaData().getURL();
            Assertions.assertTrue(jdbcUrl.contains(String.valueOf(MYSQL_CONTAINER.getMappedPort(3306))));
            Assertions.assertEquals("nfu_topic_selection", connection.getCatalog());
        }

        // 3. 校验 Redis 连接指向容器端口，且业务键前缀为 nfu:topic-selection: 无重复前缀
        Assertions.assertTrue(redisConnectionFactory instanceof LettuceConnectionFactory);
        LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) redisConnectionFactory;
        Assertions.assertEquals(REDIS_CONTAINER.getMappedPort(6379).intValue(), lettuceFactory.getPort());

        redisManager.setValue(RedisConstant.SEARCH_KEY_PREFIX + "smoke-hash", "ok", 60);
        Set<String> keys = stringRedisTemplate.keys("*");
        Assertions.assertNotNull(keys);
        Assertions.assertTrue(keys.contains("nfu:topic-selection:search:smoke-hash"));
        for (String key : keys) {
            Assertions.assertFalse(key.contains("nfu:topic-selection:nfu:topic-selection:"));
            Assertions.assertTrue(key.startsWith("nfu:topic-selection:"));
        }
    }

}
