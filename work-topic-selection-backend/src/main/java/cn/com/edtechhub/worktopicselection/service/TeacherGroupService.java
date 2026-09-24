package cn.com.edtechhub.worktopicselection.service;

import cn.com.edtechhub.worktopicselection.exception.CodeBindMessageEnums;
import cn.com.edtechhub.worktopicselection.utils.ThrowUtils;
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
        return jdbcTemplate.queryForList("SELECT q.groupName, q.maxTopics, "
                + "q.maxTopics - (SELECT COUNT(*) FROM topic t WHERE t.teacherAccount=q.teacherAccount "
                + "AND t.topicGroup=q.groupName AND t.isDelete=0) AS remaining "
                + "FROM teacher_group_quota q WHERE q.teacherAccount=? ORDER BY q.groupName", account);
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
                "SELECT q.teacherAccount AS teacherAccount, q.groupName AS groupName, q.maxTopics AS maxTopics, "
                        + "q.maxTopics - (SELECT COUNT(*) FROM topic t WHERE t.teacherAccount=q.teacherAccount "
                        + "AND t.topicGroup=q.groupName AND t.isDelete=0) AS remaining "
                        + "FROM teacher_group_quota q WHERE q.teacherAccount IN (" + placeholders + ") "
                        + "ORDER BY q.teacherAccount, q.groupName",
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
     * 基于 JdbcTemplate 通过 UNION 查询 project 与 teacher_group_quota 两表中已配置的非空选题组名称并集
     *
     * @return 排序后的全部选题组名称列表
     */
    public List<String> allGroups() {
        return jdbcTemplate.queryForList(
                "SELECT groupName FROM ("
                        + "SELECT DISTINCT groupName FROM project WHERE groupName IS NOT NULL AND groupName<>'' "
                        + "UNION SELECT DISTINCT groupName FROM teacher_group_quota WHERE groupName IS NOT NULL AND groupName<>''"
                        + ") g ORDER BY groupName", String.class);
    }

    /**
     * 在持有教师行锁的上下文中基于 JdbcTemplate 校验 teacher_group_quota 表额度配置与 topic 表已用题目数量
     *
     * @param account         教师账号
     * @param group           选题组名称
     * @param excludedTopicId 更新题目时需排除统计的题目 ID
     */
    public void validate(String account, String group, Long excludedTopicId) {
        List<Integer> limits = jdbcTemplate.queryForList(
                "SELECT maxTopics FROM teacher_group_quota WHERE teacherAccount=? AND groupName=?", Integer.class, account, group);
        ThrowUtils.throwIf(limits.size() != 1, CodeBindMessageEnums.NO_AUTH_ERROR, "请选择当前教师所属的选题组");
        Long used = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM topic WHERE teacherAccount=? AND topicGroup=? "
                + "AND isDelete=0 AND (? IS NULL OR id<>?)", Long.class, account, group, excludedTopicId, excludedTopicId);
        ThrowUtils.throwIf(used >= limits.get(0), CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "该组选题额度已用完");
    }

}
