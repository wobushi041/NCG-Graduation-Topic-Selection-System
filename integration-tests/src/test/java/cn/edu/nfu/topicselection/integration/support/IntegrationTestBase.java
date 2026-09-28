package cn.edu.nfu.topicselection.integration.support;

import cn.edu.nfu.topicselection.TopicSelectionApplication;
import cn.edu.nfu.topicselection.manager.ai.AIManager;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.annotation.Resource;

/**
 * 后端集成测试公共基类，提供单例容器生命周期、动态属性覆盖与测试前后数据清理
 *
 * @author wobushi041
 */
@SpringBootTest(classes = TopicSelectionApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
public abstract class IntegrationTestBase {

    /**
     * 单例 MySQL 8 测试容器
     */
    protected static final MySQLContainer<?> MYSQL_CONTAINER;

    /**
     * 单例 Redis 7 测试容器
     */
    protected static final GenericContainer<?> REDIS_CONTAINER;

    static {
        MYSQL_CONTAINER = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("nfu_topic_selection")
                .withUsername("test_user")
                .withPassword("test_password")
                .withInitScript("sql/schema.sql");
        MYSQL_CONTAINER.start();

        REDIS_CONTAINER = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);
        REDIS_CONTAINER.start();
    }

    /**
     * 随机分配的本地 HTTP 服务端口
     */
    @LocalServerPort
    protected int serverPort;

    /**
     * 模拟邮件发送器，防止集成测试连接真实 SMTP 服务器
     */
    @MockBean
    protected JavaMailSender javaMailSender;

    /**
     * 模拟外部 AI 管理器，防止集成测试调用真实外部 AI 服务
     */
    @MockBean
    protected AIManager aiManager;

    /**
     * 注入测试数据清理器
     */
    @Resource
    protected DatabaseCleaner databaseCleaner;

    /**
     * 注入测试数据构造工厂
     */
    @Resource
    protected TestFixtureFactory testFixtureFactory;

    /**
     * 注入 HTTP 测试客户端
     */
    @Resource
    protected TestApiClient testApiClient;

    /**
     * 动态注册容器数据库与 Redis 连接配置，覆盖主配置文件属性
     *
     * @param registry 动态属性注册器
     */
    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
        registry.add("spring.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
        registry.add("spring.redis.password", () -> "");
        registry.add("spring.redis.database", () -> 0);
    }

    /**
     * 每个测试方法执行前重置数据库表与 Redis 业务键并绑定当前端口
     */
    @BeforeEach
    public void setUpIntegrationEnvironment() {
        testApiClient.setServerPort(serverPort);
        databaseCleaner.cleanAll();
    }

}
