package cn.edu.nfu.topicselection.manager.sentine;

import cn.edu.nfu.topicselection.manager.sentinel.SentinelRuleRegistry;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Sentinel 流量控制规则管理器
 *
 * @author wobushi041
 */
@Component
@Slf4j
@Data
public class SentineManager {

    /**
     * 注入 SentineConfig 配置依赖
     */
    @Resource
    private SentineConfig sentineConfig;

    /**
     * 集中式 Sentinel 规则注册器
     */
    @Resource
    private SentinelRuleRegistry sentinelRuleRegistry;

    /**
     * 按指定资源名称与自定义阈值注册或更新 Sentinel QPS 限流规则
     *
     * @param entryName 受保护的限流资源名称
     * @param count     每秒允许的最大请求阈值（QPS）
     */
    public void initFlowRules(String entryName, Integer count) {
        sentinelRuleRegistry.register(entryName, count.doubleValue());
    }

    /**
     * 按指定资源名称与系统默认 QPS 阈值注册或更新 Sentinel 限流规则
     *
     * @param entryName 受保护的限流资源名称
     */
    public void initFlowRules(String entryName) {
        sentinelRuleRegistry.register(entryName, sentineConfig.getQps());
    }

}
