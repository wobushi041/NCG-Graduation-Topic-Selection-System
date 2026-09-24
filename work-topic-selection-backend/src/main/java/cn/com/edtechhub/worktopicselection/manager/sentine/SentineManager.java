package cn.com.edtechhub.worktopicselection.manager.sentine;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
     * 内存中的资源名称与 Sentinel 限流规则映射表
     */
    private final Map<String, FlowRule> flowRules = new ConcurrentHashMap<>();

    /**
     * 按指定资源名称与自定义阈值注册或更新 Sentinel QPS 限流规则
     *
     * @param entryName 受保护的限流资源名称
     * @param count     每秒允许的最大请求阈值（QPS）
     */
    public void initFlowRules(String entryName, Integer count) {
        upsertFlowRule(entryName, count.doubleValue());
    }

    /**
     * 按指定资源名称与系统默认 QPS 阈值注册或更新 Sentinel 限流规则
     *
     * @param entryName 受保护的限流资源名称
     */
    public void initFlowRules(String entryName) {
        upsertFlowRule(entryName, sentineConfig.getQps());
    }

    /**
     * 线程安全地新增或更新内存限流规则表并重新加载至 FlowRuleManager
     *
     * @param entryName 受保护的限流资源名称
     * @param count     每秒允许的最大请求阈值（QPS）
     */
    private synchronized void upsertFlowRule(String entryName, double count) {
        FlowRule current = flowRules.get(entryName);
        if (current != null && Double.compare(current.getCount(), count) == 0) {
            return;
        }
        FlowRule rule = new FlowRule();
        rule.setResource(entryName);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(count);
        flowRules.put(entryName, rule);
        FlowRuleManager.loadRules(new ArrayList<>(flowRules.values()));
    }

}
