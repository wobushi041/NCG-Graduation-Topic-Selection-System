package cn.edu.nfu.topicselection.utils;

import cn.edu.nfu.topicselection.exception.BusinessException;
import cn.edu.nfu.topicselection.model.request.organization.ProjectQueryRequest;
import cn.edu.nfu.topicselection.service.impl.ProjectServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * SQL 工具与查询排序安全单元测试
 *
 * @author wobushi041
 */
class SqlUtilsTest {

    /**
     * 专业服务实现实例
     */
    private final ProjectServiceImpl projectService = new ProjectServiceImpl();

    /// 排序字段与方向校验 ///

    // 场景：测试空字符串排序字段返回 false
    @Test
    void validSortField_givenEmptyString_returnsFalse() {
        // 1. 调用 validSortField 传入空字符串
        boolean result = SqlUtils.validSortField("");
        // 2. 断言结果为 false
        assertFalse(result);
    }

    // 场景：测试 null 排序字段返回 false
    @Test
    void validSortField_givenNull_returnsFalse() {
        // 1. 调用 validSortField 传入 null
        boolean result = SqlUtils.validSortField(null);
        // 2. 断言结果为 false
        assertFalse(result);
    }

    // 场景：测试纯空格排序字段返回 false
    @Test
    void validSortField_givenBlankString_returnsFalse() {
        // 1. 调用 validSortField 传入纯空格字符串
        boolean result = SqlUtils.validSortField("   ");
        // 2. 断言结果为 false
        assertFalse(result);
    }

    /**
     * 参数化验证合法与非法排序字段
     *
     * @param sortField 排序字段
     * @param expected  预期校验结果
     */
    @ParameterizedTest
    @CsvSource({
            // 合法场景
            "id, true",
            "userName, true",
            "createTime, true",
            // 非法场景（不在固定白名单中）
            "order123, false",
            "user_name, false",
            "id=1, false",
            "(id), false",
            "id), false",
            "id (, false",
            "id name, false", // 含空格
            "name=desc, false",
            "123(456), false"
    })
    void validSortField_givenVariousInputs_returnsExpectedResult(String sortField, boolean expected) {
        boolean result = SqlUtils.validSortField(sortField);
        assertEquals(expected, result, "输入：" + sortField + " 时结果不符合预期");
    }

    /**
     * 参数化验证课题排序字段白名单与注入载荷
     *
     * @param sortField 排序字段
     * @param expected  预期校验结果
     */
    @ParameterizedTest
    @CsvSource({
            "topic, true",
            "surplusQuantity, true",
            "userPassword, false",
            "id desc; drop table topic, false",
            "id/**/desc, false"
    })
    void validTopicSortField_givenWhitelistAndAttackPayloads_returnsExpectedResult(String sortField, boolean expected) {
        assertEquals(expected, SqlUtils.validTopicSortField(sortField));
    }

    /**
     * 参数化验证排序方向白名单与注入载荷
     *
     * @param sortOrder 排序方向
     * @param expected  预期校验结果
     */
    @ParameterizedTest
    @CsvSource({
            "ascend, true",
            "descend, true",
            "asc, false",
            "desc, false",
            "desc; drop table user, false",
            "'descend, nulls first', false"
    })
    void validSortOrder_givenWhitelistAndAttackPayloads_returnsExpectedResult(String sortOrder, boolean expected) {
        assertEquals(expected, SqlUtils.validSortOrder(sortOrder));
    }

    // 场景：测试专业查询传入恶意排序字段时不拼接 ORDER BY 子句
    @Test
    void getQueryWrapper_givenMaliciousSortField_doesNotAddOrderBy() {
        // 1. 构造包含恶意排序字段的查询请求
        ProjectQueryRequest request = new ProjectQueryRequest();
        request.setSortField("id desc; drop table project");

        // 2. 构建查询条件包装器
        QueryWrapper<?> wrapper = projectService.getQueryWrapper(request);

        // 3. 断言生成的 SQL 片段不包含 ORDER BY
        assertFalse(wrapper.getSqlSegment().toUpperCase().contains("ORDER BY"));
    }

    // 场景：测试专业查询传入恶意排序方向时抛出业务异常拒绝请求
    @Test
    void getQueryWrapper_givenMaliciousSortOrder_rejectsRequest() {
        // 1. 构造包含恶意排序方向的查询请求
        ProjectQueryRequest request = new ProjectQueryRequest();
        request.setSortField("id");
        request.setSortOrder("desc; drop table project");

        // 2. 断言构建查询条件时抛出 BusinessException
        assertThrows(BusinessException.class, () -> projectService.getQueryWrapper(request));
    }

}

