package cn.edu.nfu.topicselection.service;

import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import cn.edu.nfu.topicselection.utils.ThrowUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 教师选题组额度服务类
 *
 * @author wobushi041
 */
@Service
public class TeacherGroupService {

    /**
     * 注入 JdbcTemplate 依赖
     */
    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 基于 JdbcTemplate 关联查询 teacher_group_quota 与 topic 表获取指定教师各选题组总额度及剩余额度
     *
     * @param account 教师账号
     * @return 教师各选题组总额度与剩余额度列表
     */
    public List<Map<String, Object>> groups(String account) {
        return jdbcTemplate.queryForList("SELECT q.topicGroupId, g.groupName, q.maxTopics, "
                + "q.maxTopics - (SELECT COUNT(*) FROM topic t WHERE t.teacherAccount=q.teacherAccount "
                + "AND t.topicGroupId=q.topicGroupId AND t.isDelete=0) AS remaining "
                + "FROM teacher_group_quota q JOIN topic_group g ON g.id=q.topicGroupId "
                + "WHERE q.teacherAccount=? ORDER BY g.groupName", account);
    }

    /**
     * 基于 JdbcTemplate 使用 IN 子句批量查询多名教师在 teacher_group_quota 与 topic 表中的选题组额度及剩余量
     *
     * @param accounts 教师账号列表
     * @return 按教师账号分组的选题组额度映射表
     */
    public Map<String, List<Map<String, Object>>> groupsBatch(List<String> accounts) {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        if (accounts == null) {
            return result;
        }
        List<String> distinctAccounts = accounts.stream()
                .filter(account -> account != null && !account.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
        for (String account : distinctAccounts) {
            result.put(account, new ArrayList<>());
        }
        if (distinctAccounts.isEmpty()) {
            return result;
        }
        String placeholders = distinctAccounts.stream().map(item -> "?").collect(Collectors.joining(","));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT q.teacherAccount AS teacherAccount, q.topicGroupId AS topicGroupId, "
                        + "g.groupName AS groupName, q.maxTopics AS maxTopics, "
                        + "q.maxTopics - (SELECT COUNT(*) FROM topic t WHERE t.teacherAccount=q.teacherAccount "
                        + "AND t.topicGroupId=q.topicGroupId AND t.isDelete=0) AS remaining "
                        + "FROM teacher_group_quota q JOIN topic_group g ON g.id=q.topicGroupId "
                        + "WHERE q.teacherAccount IN (" + placeholders + ") "
                        + "ORDER BY q.teacherAccount, g.groupName",
                distinctAccounts.toArray());
        for (Map<String, Object> row : rows) {
            Object accountValue = row.get("teacherAccount");
            if (accountValue == null) {
                continue;
            }
            String account = String.valueOf(accountValue);
            result.computeIfAbsent(account, key -> new ArrayList<>()).add(row);
        }
        return result;
    }

    /**
     * 基于 JdbcTemplate 查询选题组表中的有效选题组名称
     *
     * @return 排序后的全部选题组名称列表
     */
    public List<String> allGroups() {
        return jdbcTemplate.queryForList(
                "SELECT groupName FROM topic_group WHERE isDelete=0 ORDER BY groupName", String.class);
    }

    /**
     * 查询指定选题组中已配置额度的教师账号
     *
     * @param topicGroupId 选题组 id
     * @return 教师账号列表
     */
    public List<String> teacherAccountsForGroup(Long topicGroupId) {
        return jdbcTemplate.queryForList(
                "SELECT teacherAccount FROM teacher_group_quota WHERE topicGroupId=? ORDER BY teacherAccount",
                String.class, topicGroupId);
    }

    /**
     * 校验当前已出题目数量后更新教师在指定选题组中的最大出题数量
     *
     * @param account      教师账号
     * @param topicGroupId 选题组 id
     * @param maxTopics    最大出题数量
     */
    public void updateQuota(String account, Long topicGroupId, Integer maxTopics) {
        List<Integer> limits = jdbcTemplate.queryForList(
                "SELECT maxTopics FROM teacher_group_quota WHERE teacherAccount=? AND topicGroupId=?",
                Integer.class, account, topicGroupId);
        ThrowUtils.throwIf(limits.size() != 1, CodeBindMessageEnums.NOT_FOUND_ERROR,
                "教师未配置该选题组额度");
        Long used = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM topic WHERE teacherAccount=? AND topicGroupId=? AND isDelete=0",
                Long.class, account, topicGroupId);
        ThrowUtils.throwIf(used != null && maxTopics < used, CodeBindMessageEnums.PARAMS_ERROR,
                "最大出题数量不能小于当前已出题目数量(" + used + ")");
        int updatedRows = jdbcTemplate.update(
                "UPDATE teacher_group_quota SET maxTopics=? WHERE teacherAccount=? AND topicGroupId=?",
                maxTopics, account, topicGroupId);
        ThrowUtils.throwIf(updatedRows != 1, CodeBindMessageEnums.OPERATION_ERROR,
                "更新教师选题组额度失败");
    }

    /**
     * 在持有教师行锁的上下文中基于 JdbcTemplate 校验 teacher_group_quota 表额度配置与 topic 表已用题目数量
     *
     * @param account         教师账号
     * @param topicGroupId    选题组 id
     * @param excludedTopicId 更新题目时需排除统计的题目 ID
     */
    public void validate(String account, Long topicGroupId, Long excludedTopicId) {
        List<Integer> limits = jdbcTemplate.queryForList(
                "SELECT maxTopics FROM teacher_group_quota WHERE teacherAccount=? AND topicGroupId=?",
                Integer.class, account, topicGroupId);
        ThrowUtils.throwIf(limits.size() != 1, CodeBindMessageEnums.NO_AUTH_ERROR, "请选择当前教师所属的选题组");
        Long used = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM topic WHERE teacherAccount=? AND topicGroupId=? "
                        + "AND isDelete=0 AND (? IS NULL OR id<>?)", Long.class, account, topicGroupId,
                excludedTopicId, excludedTopicId);
        ThrowUtils.throwIf(used >= limits.get(0), CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "该组选题额度已用完");
    }

}
