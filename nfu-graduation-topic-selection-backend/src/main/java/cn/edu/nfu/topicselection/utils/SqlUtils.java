package cn.edu.nfu.topicselection.utils;

import cn.edu.nfu.topicselection.constant.CommonConstant;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * SQL 排序字段安全校验工具类
 *
 * @author wobushi041
 */
public class SqlUtils {

    /**
     * 用户表允许排序的字段集合
     */
    private static final Set<String> USER_SORT_FIELDS = fields(
            "id", "userAccount", "userName", "createTime", "updateTime",
            "userRole", "dept", "status", "project", "topicAmount"
    );

    /**
     * 选题表允许排序的字段集合
     */
    private static final Set<String> TOPIC_SORT_FIELDS = fields(
            "id", "topic", "type", "teacherName", "deptName", "deptTeacher",
            "createTime", "updateTime", "surplusQuantity", "startTime", "endTime",
            "status", "selectAmount"
    );

    /**
     * 专业表允许排序的字段集合
     */
    private static final Set<String> PROJECT_SORT_FIELDS = fields(
            "id", "projectName", "deptName", "createTime", "updateTime"
    );

    /**
     * 系部表允许排序的字段集合
     */
    private static final Set<String> DEPT_SORT_FIELDS = fields(
            "id", "deptName", "createTime", "updateTime"
    );

    /**
     * 校验默认排序字段是否合法（防止 SQL 注入）
     *
     * @param sortField 排序字段名称
     * @return 是否为合法排序字段
     */
    public static boolean validSortField(String sortField) {
        return validUserSortField(sortField);
    }

    /**
     * 校验用户表排序字段是否合法
     *
     * @param sortField 排序字段名称
     * @return 是否为合法用户排序字段
     */
    public static boolean validUserSortField(String sortField) {
        return USER_SORT_FIELDS.contains(sortField);
    }

    /**
     * 校验选题表排序字段是否合法
     *
     * @param sortField 排序字段名称
     * @return 是否为合法选题排序字段
     */
    public static boolean validTopicSortField(String sortField) {
        return TOPIC_SORT_FIELDS.contains(sortField);
    }

    /**
     * 校验专业表排序字段是否合法
     *
     * @param sortField 排序字段名称
     * @return 是否为合法专业排序字段
     */
    public static boolean validProjectSortField(String sortField) {
        return PROJECT_SORT_FIELDS.contains(sortField);
    }

    /**
     * 校验系部表排序字段是否合法
     *
     * @param sortField 排序字段名称
     * @return 是否为合法系部排序字段
     */
    public static boolean validDeptSortField(String sortField) {
        return DEPT_SORT_FIELDS.contains(sortField);
    }

    /**
     * 校验排序顺序关键字是否合法
     *
     * @param sortOrder 排序顺序（ascend 或 descend）
     * @return 是否为合法排序顺序
     */
    public static boolean validSortOrder(String sortOrder) {
        return CommonConstant.SORT_ORDER_ASC.equals(sortOrder)
                || CommonConstant.SORT_ORDER_DESC.equals(sortOrder);
    }

    /**
     * 构建不可变的允许排序字段集合
     *
     * @param fields 字段名称可变参数
     * @return 不可变字段集合
     */
    private static Set<String> fields(String... fields) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(fields)));
    }

}
