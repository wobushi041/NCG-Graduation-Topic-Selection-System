package cn.com.edtechhub.worktopicselection.manager.sentinel;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 集中维护并一次性发布 Sentinel 流控规则。
 */
@Component
public class SentinelRuleRegistry {

    private final SentinelRateLimitProperties properties;
    private final Map<String, FlowRule> rules = new ConcurrentHashMap<>();

    public SentinelRuleRegistry(SentinelRateLimitProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void registerAuthenticationRules() {
        register("auth.login", properties.getLogin(), false);
        register("auth.logout", properties.getLogout(), false);
        register("auth.role-switch", properties.getRoleSwitch(), false);
        register("auth.role-switch-availability", properties.getRoleSwitchAvailability(), false);
        register("auth.password-admin-reset", properties.getPasswordAdminReset(), false);
        register("auth.password-change", properties.getPasswordChange(), false);
        register("auth.password-reset", properties.getPasswordReset(), false);
        register("auth.password-reset-code-send", properties.getPasswordResetCodeSend(), false);
        register("auth.email-code-send", properties.getEmailCodeSend(), false);
        register("auth.email-code-verify", properties.getEmailCodeVerify(), false);
        publish();
    }

    public synchronized void register(String resource, double qps) {
        register(resource, qps, true);
    }

    private void register(String resource, double qps, boolean publishImmediately) {
        FlowRule current = rules.get(resource);
        if (current != null && Double.compare(current.getCount(), qps) == 0) {
            return;
        }
        FlowRule rule = new FlowRule(resource);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(qps);
        rules.put(resource, rule);
        if (publishImmediately) {
            publish();
        }
    }

    private void publish() {
        FlowRuleManager.loadRules(new ArrayList<>(rules.values()));
    }
}
