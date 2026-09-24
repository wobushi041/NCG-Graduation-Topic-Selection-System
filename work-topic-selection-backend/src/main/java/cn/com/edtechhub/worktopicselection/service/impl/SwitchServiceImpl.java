package cn.com.edtechhub.worktopicselection.service.impl;

import cn.com.edtechhub.worktopicselection.manager.redis.RedisManager;
import cn.com.edtechhub.worktopicselection.service.SwitchService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 系统功能开关服务实现类
 * TODO: 不过实际上这是没有必要的, Redis 本身就是线程安全的, 不需要使用锁, 不过先先写着吧(而且这个锁也只是本地锁, 集群会出现问题)
 *
 * @author wobushi041
 */
@Service
public class SwitchServiceImpl implements SwitchService {

    /**
     * 注入 Redis 缓存管理器依赖
     */
    @Resource
    private RedisManager redisManager;

    /**
     * 保证跨选开关读写操作串行化的本地可重入锁
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * 在 ReentrantLock 同步锁保护下从 RedisManager 读取指定键的字符串值并解析为布尔开关状态
     *
     * @param key 功能开关标识键名
     * @return 是否处于开启状态
     */
    @Override
    public boolean isEnabled(String key) {
        lock.lock();
        try {
            String val = redisManager.getValue(key);
            return "true".equalsIgnoreCase(val); // 字符串转 boolean
        } finally {
            lock.unlock();
        }
    }

    /**
     * 在 ReentrantLock 同步锁保护下将布尔开关状态序列化为字符串并写入 RedisManager
     *
     * @param key     功能开关标识键名
     * @param enabled 是否开启功能开关
     */
    @Override
    public void setEnabled(String key, boolean enabled) {
        lock.lock();
        try {
            redisManager.setValue(key, Boolean.toString(enabled)); // boolean 转字符串
        } finally {
            lock.unlock();
        }
    }

}
