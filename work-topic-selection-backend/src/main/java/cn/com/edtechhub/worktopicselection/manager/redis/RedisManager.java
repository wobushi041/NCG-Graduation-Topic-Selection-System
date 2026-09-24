package cn.com.edtechhub.worktopicselection.manager.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Resource;

/**
 * Redis 分布式缓存与 Lua 原子脚本操作管理器
 *
 * @author wobushi041
 */
@Component
public class RedisManager {

    /**
     * 原子读取并校验删除一次性缓存值的 Lua 脚本
     */
    private static final DefaultRedisScript<Long> CONSUME_VALUE_SCRIPT = new DefaultRedisScript<>(
            "local value = redis.call('GET', KEYS[1]); " +
                    "if not value then return 0; end; " +
                    "redis.call('DEL', KEYS[1]); " +
                    "if value == ARGV[1] then return 1; end; " +
                    "return -1;",
            Long.class
    );

    /**
     * 固定时间窗口原子递增计数与首次过期设置的 Lua 限流脚本
     */
    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]); " +
                    "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]); end; " +
                    "return count;",
            Long.class
    );

    /**
     * 原子归还已占用限流配额并保留原窗口过期时间的 Lua 脚本
     */
    private static final DefaultRedisScript<Long> RELEASE_RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
            "local value = redis.call('GET', KEYS[1]); " +
                    "if not value then return 0; end; " +
                    "local count = tonumber(value); " +
                    "if count <= 1 then redis.call('DEL', KEYS[1]); return 0; end; " +
                    "return redis.call('DECR', KEYS[1]);",
            Long.class
    );

    /**
     * 注入 RedisConfig 配置依赖
     */
    @Resource
    private RedisConfig redisConfig;

    /**
     * 注入 StringRedisTemplate 模板依赖
     */
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 写入带过期时间的字符串缓存值（自动拼接业务键前缀）
     *
     * @param key            缓存键名
     * @param value          字符串缓存值
     * @param timeoutSeconds 过期时间（秒）
     */
    public void setValue(String key, String value, long timeoutSeconds) {
        stringRedisTemplate.opsForValue().set(redisConfig.getKeyPrefix() + key, value, timeoutSeconds, TimeUnit.SECONDS);
    }

    /**
     * 写入永久有效的字符串缓存值（自动拼接业务键前缀）
     *
     * @param key   缓存键名
     * @param value 字符串缓存值
     */
    public void setValue(String key, String value) {
        stringRedisTemplate.opsForValue().set(redisConfig.getKeyPrefix() + key, value);
    }

    /**
     * 根据键名读取 Redis 字符串缓存值
     *
     * @param key 缓存键名
     * @return 命中的字符串缓存值，不存在时返回 null
     */
    public String getValue(String key) {
        return stringRedisTemplate.opsForValue().get(redisConfig.getKeyPrefix() + key);
    }

    /**
     * 根据键名删除指定的 Redis 缓存键
     *
     * @param key 缓存键名
     */
    public void deleteKey(String key) {
        stringRedisTemplate.delete(redisConfig.getKeyPrefix() + key);
    }

    /**
     * 通过 Lua 脚本原子读取、校验并删除一次性缓存值
     *
     * @param key           缓存键名
     * @param expectedValue 期望匹配的目标值
     * @return 匹配结果状态码（1 表示匹配成功并删除，0 表示键不存在，-1 表示值不匹配且已删除）
     */
    public long consumeValue(String key, String expectedValue) {
        Long result = stringRedisTemplate.execute(
                CONSUME_VALUE_SCRIPT,
                Collections.singletonList(redisConfig.getKeyPrefix() + key),
                expectedValue
        );
        return result == null ? 0L : result;
    }

    /**
     * 基于 Redis Lua 脚本执行固定时间窗口原子计数限流
     *
     * @param key           限流业务键名
     * @param maxAttempts   时间窗口内允许的最大请求次数
     * @param windowSeconds 限流时间窗口大小（秒）
     * @return 是否成功获取限流通行许可
     */
    public boolean tryAcquire(String key, int maxAttempts, long windowSeconds) {
        Long count = stringRedisTemplate.execute(
                RATE_LIMIT_SCRIPT,
                Collections.singletonList(redisConfig.getKeyPrefix() + key),
                String.valueOf(windowSeconds)
        );
        return count != null && count <= maxAttempts;
    }

    /**
     * 通过 Lua 脚本原子归还一次已占用的限流额度并保留原窗口过期时间
     *
     * @param key 限流业务键名
     */
    public void releaseRateLimit(String key) {
        stringRedisTemplate.execute(
                RELEASE_RATE_LIMIT_SCRIPT,
                Collections.singletonList(redisConfig.getKeyPrefix() + key)
        );
    }

    /**
     * 按通配符模式批量检索匹配的 Redis 键名集合（剥离业务键前缀）
     *
     * @param pattern 键名通配符匹配模式
     * @return 剥离前缀后的匹配键名集合
     */
    public Set<String> getKeysByPattern(String pattern) {
        Set<String> keysWithPrefix = stringRedisTemplate.keys(redisConfig.getKeyPrefix() + pattern);
        if (keysWithPrefix == null) {
            return Collections.emptySet();
        }
        // 去掉前缀再返回
        return keysWithPrefix
                .stream()
                .map(key -> key.replaceFirst(redisConfig.getKeyPrefix(), ""))
                .collect(Collectors.toSet());
    }

    /**
     * 拼接业务键前缀后批量删除指定的 Redis 缓存键集合
     *
     * @param keys 待删除的缓存键名集合
     */
    public void deleteKeys(Collection<String> keys) {
        if (keys != null && !keys.isEmpty()) {
            keys = keys
                    .stream()
                    .map(key -> redisConfig.getKeyPrefix() + key)
                    .collect(Collectors.toSet());
            stringRedisTemplate.delete(keys);
        }
    }

    /**
     * 向 Redis List 列表尾部追加字符串元素
     *
     * @param key   列表缓存键名
     * @param value 待追加的字符串元素
     */
    public void rightPushList(String key, String value) {
        stringRedisTemplate.opsForList().rightPush(redisConfig.getKeyPrefix() + key, value);
    }

    /**
     * 从 Redis List 列表头部弹出并返回首个字符串元素
     *
     * @param key 列表缓存键名
     * @return 列表头部弹出的字符串元素，列表为空时返回 null
     */
    public String leftPopList(String key) {
        return stringRedisTemplate.opsForList().leftPop(redisConfig.getKeyPrefix() + key);
    }

    /**
     * 向 Redis Set 无序集合中批量添加字符串元素
     *
     * @param key    集合缓存键名
     * @param values 待添加的多个字符串元素
     */
    public void addSet(String key, String... values) {
        stringRedisTemplate.opsForSet().add(redisConfig.getKeyPrefix() + key, values);
    }

    /**
     * 获取 Redis Set 无序集合中的所有成员元素
     *
     * @param key 集合缓存键名
     * @return 集合中的所有成员对象
     */
    public Object getSetMembers(String key) {
        return stringRedisTemplate.opsForSet().members(redisConfig.getKeyPrefix() + key);
    }

    /**
     * 向 Redis Hash 哈希表中写入指定字段的值
     *
     * @param key   哈希缓存键名
     * @param field 哈希表字段名
     * @param value 字段对应的字符串值
     */
    public void putHash(String key, String field, String value) {
        stringRedisTemplate.opsForHash().put(redisConfig.getKeyPrefix() + key, field, value);
    }

    /**
     * 从 Redis Hash 哈希表中读取指定字段的值
     *
     * @param key   哈希缓存键名
     * @param field 哈希表字段名
     * @return 字段对应的缓存值对象，不存在时返回 null
     */
    public Object getHash(String key, String field) {
        return stringRedisTemplate.opsForHash().get(redisConfig.getKeyPrefix() + key, field);
    }

}
