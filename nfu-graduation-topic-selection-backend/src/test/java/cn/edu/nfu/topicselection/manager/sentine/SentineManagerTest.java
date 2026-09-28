package cn.edu.nfu.topicselection.manager.sentine;

import cn.edu.nfu.topicselection.manager.sentinel.SentinelRateLimitProperties;
import cn.edu.nfu.topicselection.manager.sentinel.SentinelRuleRegistry;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sentinel 兼容管理器测试
 *
 * @author wobushi041
 */
class SentineManagerTest {

    @AfterEach
    void clearGlobalRules() {
        FlowRuleManager.loadRules(Collections.emptyList());
    }

    // 场景：测试连续注册资源时保留已经存在的 Sentinel 规则
    @Test
    void addingAResourceDoesNotReplaceExistingRules() {
        // 1. 准备集中式 Sentinel 规则注册器
        SentinelRuleRegistry manager = new SentinelRuleRegistry(new SentinelRateLimitProperties());

        // 2. 连续注册两个独立资源
        manager.register("first-resource", 10);
        manager.register("second-resource", 20);

        // 3. 断言两个资源规则同时存在
        List<String> resources = FlowRuleManager.getRules()
                .stream()
                .map(FlowRule::getResource)
                .collect(Collectors.toList());
        assertTrue(resources.contains("first-resource"));
        assertTrue(resources.contains("second-resource"));
    }

}
