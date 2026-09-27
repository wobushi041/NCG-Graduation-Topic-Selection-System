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
 * 集中维护并一次性发布 Sentinel 流控规则
 *
 * @author wobushi041
 */
@Component
public class SentinelRuleRegistry {

    /**
     * 注入认证接口限流配置
     */
    private final SentinelRateLimitProperties properties;

    /**
     * Sentinel 流控规则集合
     */
    private final Map<String, FlowRule> rules = new ConcurrentHashMap<>();

    /**
     * 初始化 Sentinel 规则注册器
     *
     * @param properties 认证接口限流配置
     */
    public SentinelRuleRegistry(SentinelRateLimitProperties properties) {
        this.properties = properties;
    }

    /// 预置规则 ///

    /**
     * 注册并发布认证模块与选题写用例模块的全部 Sentinel 流控规则
     */
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
        register("topic.selection.preselect", properties.getTopicSelectionPreselect(), false);
        register("topic.selection.confirm", properties.getTopicSelectionConfirm(), false);
        register("topic.selection.assign-student", properties.getTopicSelectionAssignStudent(), false);
        register("topic.selection.withdraw", properties.getTopicSelectionWithdraw(), false);
        register("topic.selection.query-selected-students", properties.getTopicSelectionQuerySelectedStudents(), false);
        register("topic.selection.query-preselected", properties.getTopicSelectionQueryPreselected(), false);
        register("topic.selection.query-selected", properties.getTopicSelectionQuerySelected(), false);
        register("topic.selection.query-choice-time", properties.getTopicSelectionQueryChoiceTime(), false);
        register("topic.selection.query-students-by-topic", properties.getTopicSelectionQueryStudentsByTopic(), false);
        register("topic.add", properties.getTopicAdd(), false);
        register("topic.delete", properties.getTopicDelete(), false);
        register("topic.quota.get", properties.getTopicQuotaGet(), false);
        register("topic.quota.set", properties.getTopicQuotaSet(), false);
        register("topic.review.check", properties.getTopicReviewCheck(), false);
        register("topic.publication.publish", properties.getTopicPublicationPublish(), false);
        register("topic.publication.unpublish", properties.getTopicPublicationUnpublish(), false);
        register("topic.update", properties.getTopicUpdate(), false);
        register("topic.review.ai-level", properties.getTopicReviewAiLevel(), false);
        register("organization.dept.add", properties.getOrganizationDeptAdd(), false);
        register("organization.project.add", properties.getOrganizationProjectAdd(), false);
        register("organization.project.update-group", properties.getOrganizationProjectUpdateGroup(), false);
        register("organization.dept.delete", properties.getOrganizationDeptDelete(), false);
        register("organization.project.delete", properties.getOrganizationProjectDelete(), false);
        register("organization.dept.query-page", properties.getOrganizationDeptQueryPage(), false);
        register("organization.dept.query-list", properties.getOrganizationDeptQueryList(), false);
        register("organization.project.query-page", properties.getOrganizationProjectQueryPage(), false);
        register("organization.project.query-list", properties.getOrganizationProjectQueryList(), false);
        register("teacher-group.query-self", properties.getTeacherGroupQuerySelf(), false);
        register("teacher-group.query-batch", properties.getTeacherGroupQueryBatch(), false);
        register("teacher-group.query-all", properties.getTeacherGroupQueryAll(), false);
        register("policy.cross-topic.query", properties.getPolicyCrossTopicQuery(), false);
        register("policy.cross-topic.update", properties.getPolicyCrossTopicUpdate(), false);
        register("policy.view-topic.query", properties.getPolicyViewTopicQuery(), false);
        register("policy.view-topic.update", properties.getPolicyViewTopicUpdate(), false);
        register("policy.single-choice.query", properties.getPolicySingleChoiceQuery(), false);
        register("policy.single-choice.update", properties.getPolicySingleChoiceUpdate(), false);
        register("policy.topic-lock.query", properties.getPolicyTopicLockQuery(), false);
        register("policy.topic-lock.update", properties.getPolicyTopicLockUpdate(), false);
        register("policy.dept-config.query", properties.getPolicyDeptConfigQuery(), false);
        register("policy.dept-config.update", properties.getPolicyDeptConfigUpdate(), false);
        register("policy.dept-config.delete", properties.getPolicyDeptConfigDelete(), false);
        register("system.diagnostics.test", properties.getSystemDiagnosticsTest(), false);
        register("system.info.query", properties.getSystemInfoQuery(), false);
        register("user.manage.add", properties.getUserManageAdd(), false);
        register("user.manage.delete", properties.getUserManageDelete(), false);
        register("user.manage.update", properties.getUserManageUpdate(), false);
        register("user.query.current", properties.getUserQueryCurrent(), false);
        register("user.query.page", properties.getUserQueryPage(), false);
        register("user.query.teacher-list", properties.getUserQueryTeacherList(), false);
        register("user.query.by-id", properties.getUserQueryById(), false);
        register("user.query.vo-by-id", properties.getUserQueryVoById(), false);
        register("query.topic.page", properties.getQueryTopicPage(), false);
        register("query.selection.situation", properties.getQuerySelectionSituation(), false);
        register("query.dept.teacher", properties.getQueryDeptTeacher(), false);
        register("query.selection.unselected-students", properties.getQuerySelectionUnselectedStudents(), false);
        register("query.topic.admin-page", properties.getQueryTopicAdminPage(), false);
        register("query.user.vo-page", properties.getQueryUserVoPage(), false);
        register("query.user.name-list", properties.getQueryUserNameList(), false);
        register("query.dept.pending-teacher", properties.getQueryDeptPendingTeacher(), false);
        publish();
    }

    /// 动态注册 ///

    /**
     * 注册单个 Sentinel 流控规则并立即发布规则快照
     *
     * @param resource Sentinel 资源名称
     * @param qps      QPS 阈值
     */
    public synchronized void register(String resource, double qps) {
        register(resource, qps, true);
    }

    /**
     * 更新单个 Sentinel 流控规则并按需发布规则快照
     *
     * @param resource           Sentinel 资源名称
     * @param qps                QPS 阈值
     * @param publishImmediately 是否立即发布规则快照
     */
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

    /**
     * 将当前规则快照发布到 Sentinel
     */
    private void publish() {
        FlowRuleManager.loadRules(new ArrayList<>(rules.values()));
    }

}
