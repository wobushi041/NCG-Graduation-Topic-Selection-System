package cn.com.edtechhub.worktopicselection.manager.redis;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Redis 分布式缓存配置属性类
 *
 * @author wobushi041
 */
@Component
@Data
@Slf4j
public class RedisConfig {

    /**
     * Redis 缓存键名前缀
     */
    private String keyPrefix = "work-topic-selection:";

    /**
     * 容器初始化完成后输出 Redis 键前缀配置日志
     */
    @PostConstruct
    public void printConfig() {
        log.debug("[RedisConfig] keyPrefix: {}", this.keyPrefix);
    }

}
