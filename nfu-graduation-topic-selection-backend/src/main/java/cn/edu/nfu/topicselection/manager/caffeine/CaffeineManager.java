package cn.edu.nfu.topicselection.manager.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * Caffeine 本地缓存操作管理器
 *
 * @author wobushi041
 */
@Component
@Slf4j
public class CaffeineManager {

    /**
     * 注入 CaffeineConfig 配置依赖
     */
    @Resource
    private CaffeineConfig caffeineConfig;

    /**
     * Caffeine 本地缓存实例对象
     */
    private Cache<String, Object> cache;

    /**
     * 基于配置参数构建并初始化 Caffeine 本地缓存实例
     */
    @PostConstruct
    public void init() {
        this.cache = Caffeine
                .newBuilder()
                .initialCapacity(caffeineConfig.getInitialCapacity())
                .maximumSize(caffeineConfig.getMaximumSize())
                .expireAfterWrite(caffeineConfig.getExpireAfterWrite(), TimeUnit.SECONDS)
                .build();
    }

    /**
     * 将键值对写入 Caffeine 本地缓存（自动拼接业务键前缀）
     *
     * @param key   缓存键名
     * @param value 缓存对象值
     */
    public void put(String key, Object value) {
        this.cache.put(caffeineConfig.getKeyPrefix() + key, value);
    }

    /**
     * 根据键名从 Caffeine 本地缓存读取对应值
     *
     * @param key 缓存键名
     * @return 命中的缓存对象值，未命中时返回 null
     */
    public Object get(String key) {
        return this.cache.getIfPresent(caffeineConfig.getKeyPrefix() + key);
    }

    /**
     * 根据键名使 Caffeine 本地缓存中的指定条目失效
     *
     * @param key 缓存键名
     */
    public void remove(String key) {
        this.cache.invalidate(caffeineConfig.getKeyPrefix() + key);
    }

    /**
     * 清空 Caffeine 本地缓存中的所有缓存条目
     */
    public void clearAll() {
        this.cache.invalidateAll();
    }

    /**
     * 导出当前 Caffeine 本地缓存的只读键值映射视图
     *
     * @return 包含所有当前缓存条目的 Map 集合
     */
    public Map<String, Object> dumpCache() {
        return this.cache.asMap();
    }

}
