package cn.com.edtechhub.worktopicselection.manager.caffeine;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Caffeine 本地缓存配置属性类
 *
 * @author wobushi041
 */
@Component
@Data
@Slf4j
public class CaffeineConfig {

    /**
     * 本地缓存键名前缀
     */
    private String keyPrefix = "work-topic-selection:";

    /**
     * 本地缓存初始容量大小
     */
    private Integer initialCapacity = 1024;

    /**
     * 本地缓存最大条目数量
     */
    private Long maximumSize = 10000L;

    /**
     * 写入后过期时间（秒）
     */
    private Integer expireAfterWrite = 10;

    /**
     * 容器初始化完成后输出 Caffeine 本地缓存核心配置参数日志
     */
    @PostConstruct
    public void printConfig() {
        log.debug("[CaffeineConfig] 当前项目 Caffeine 初始大小为 {}", this.initialCapacity);
        log.debug("[CaffeineConfig] 当前项目 Caffeine 最大缓存为 {}", this.maximumSize);
        log.debug("[CaffeineConfig] 当前项目 Caffeine 过期时间为 {}", this.expireAfterWrite);
    }

}
