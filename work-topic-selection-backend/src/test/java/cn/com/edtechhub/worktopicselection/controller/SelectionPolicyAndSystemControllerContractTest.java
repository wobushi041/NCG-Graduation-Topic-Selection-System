package cn.com.edtechhub.worktopicselection.controller;

import cn.com.edtechhub.worktopicselection.annotation.SentinelRateLimit;
import cn.com.edtechhub.worktopicselection.model.entity.User;
import cn.com.edtechhub.worktopicselection.model.request.policy.SetDeptConfigRequest;
import cn.com.edtechhub.worktopicselection.model.request.user.DeleteRequest;
import cn.com.edtechhub.worktopicselection.model.request.user.TeacherQueryRequest;
import cn.com.edtechhub.worktopicselection.model.request.user.UserAddRequest;
import cn.com.edtechhub.worktopicselection.model.request.user.UserQueryRequest;
import cn.com.edtechhub.worktopicselection.model.request.user.UserUpdateRequest;
import cn.com.edtechhub.worktopicselection.model.vo.DeptConfigVO;
import cn.com.edtechhub.worktopicselection.model.vo.LoginUserVO;
import cn.com.edtechhub.worktopicselection.model.vo.TeacherVO;
import cn.com.edtechhub.worktopicselection.model.vo.TheSystemInfoVO;
import cn.com.edtechhub.worktopicselection.model.vo.TopicLockVO;
import cn.com.edtechhub.worktopicselection.model.vo.UserVO;
import cn.com.edtechhub.worktopicselection.response.BaseResponse;
import cn.com.edtechhub.worktopicselection.service.SelectionPolicyService;
import cn.com.edtechhub.worktopicselection.service.UserApplicationService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 系统策略、系统诊断与用户管理控制器契约单元测试
 *
 * @author wobushi041
 */
@ExtendWith(MockitoExtension.class)
class SelectionPolicyAndSystemControllerContractTest {

    /**
     * 模拟选题开关与策略服务依赖
     */
    @Mock
    private SelectionPolicyService selectionPolicyService;

    /**
     * 模拟用户应用服务依赖
     */
    @Mock
    private UserApplicationService userApplicationService;

    /**
     * 待测系统策略控制器实例
     */
    @InjectMocks
    private SelectionPolicyController selectionPolicyController;

    /**
     * 待测系统诊断与监控控制器实例
     */
    @InjectMocks
    private SystemController systemController;

    /**
     * 待测用户管理控制器实例
     */
    @InjectMocks
    private UserController userController;

    // 场景：测试 SelectionPolicyController 与 SystemController 全部 13 个端点的鉴权与限流注解
    @Test
    void policyAndSystemEndpoints_shouldDeclareAuthAndRateLimitAnnotations() throws Exception {
        // 1. 准备测试数据
        Method getCrossMethod = SelectionPolicyController.class.getMethod("getCrossTopicStatus");
        Method setCrossMethod = SelectionPolicyController.class.getMethod("setCrossTopicStatus", boolean.class);
        Method getViewMethod = SelectionPolicyController.class.getMethod("getViewTopicStatus");
        Method setViewMethod = SelectionPolicyController.class.getMethod("setViewTopicStatus", boolean.class);
        Method getSingleMethod = SelectionPolicyController.class.getMethod("getSwitchSingleChoiceStatus");
        Method setSingleMethod = SelectionPolicyController.class.getMethod("setSwitchSingleChoiceStatus", boolean.class);
        Method getLockMethod = SelectionPolicyController.class.getMethod("getTopicLock");
        Method setLockMethod = SelectionPolicyController.class.getMethod("setTopicLock", boolean.class, String.class);
        Method getDeptCfgMethod = SelectionPolicyController.class.getMethod("getDeptConfig");
        Method setDeptCfgMethod = SelectionPolicyController.class.getMethod("setDeptConfig", SetDeptConfigRequest.class);
        Method delDeptCfgMethod = SelectionPolicyController.class.getMethod("delDeptConfig");
        Method testMethod = SystemController.class.getMethod("test");
        Method getSystemInfoMethod = SystemController.class.getMethod("getSystemInfo");

        // 2. 调用反射检查注解
        assertNotNull(getCrossMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(setCrossMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(getViewMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(setViewMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(getSingleMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(setSingleMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(getLockMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(setLockMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(getDeptCfgMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(setDeptCfgMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(delDeptCfgMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(testMethod.getAnnotation(SaIgnore.class));
        assertNotNull(getSystemInfoMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(getSystemInfoMethod.getAnnotation(SaCheckRole.class));

        // 3. 断言 Sentinel 资源名称正确
        assertEquals("policy.cross-topic.query", getCrossMethod.getAnnotation(SentinelRateLimit.class).resource());
        assertEquals("policy.cross-topic.update", setCrossMethod.getAnnotation(SentinelRateLimit.class).resource());
        assertEquals("policy.view-topic.query", getViewMethod.getAnnotation(SentinelRateLimit.class).resource());
        assertEquals("policy.view-topic.update", setViewMethod.getAnnotation(SentinelRateLimit.class).resource());
        assertEquals("policy.single-choice.query", getSingleMethod.getAnnotation(SentinelRateLimit.class).resource());
        assertEquals("policy.single-choice.update", setSingleMethod.getAnnotation(SentinelRateLimit.class).resource());
        assertEquals("policy.topic-lock.query", getLockMethod.getAnnotation(SentinelRateLimit.class).resource());
        assertEquals("policy.topic-lock.update", setLockMethod.getAnnotation(SentinelRateLimit.class).resource());
        assertEquals("policy.dept-config.query", getDeptCfgMethod.getAnnotation(SentinelRateLimit.class).resource());
        assertEquals("policy.dept-config.update", setDeptCfgMethod.getAnnotation(SentinelRateLimit.class).resource());
        assertEquals("policy.dept-config.delete", delDeptCfgMethod.getAnnotation(SentinelRateLimit.class).resource());
        assertEquals("system.diagnostics.test", testMethod.getAnnotation(SentinelRateLimit.class).resource());
        assertEquals("system.info.query", getSystemInfoMethod.getAnnotation(SentinelRateLimit.class).resource());
    }

    // 场景：测试 UserController 中 USR-001 ~ USR-008 端点的鉴权与限流注解以及零 TransactionTemplate/Mapper 字段
    @Test
    void userControllerEndpoints_shouldDeclareRateLimitsAndHaveZeroTransactionOrMapperFields() throws Exception {
        // 1. 准备测试数据
        Method addUserMethod = UserController.class.getMethod("addUser", UserAddRequest.class);
        Method deleteUserMethod = UserController.class.getMethod("deleteUser", DeleteRequest.class);
        Method updateUserMethod = UserController.class.getMethod("updateUser", UserUpdateRequest.class);
        Method getLoginUserMethod = UserController.class.getMethod("getLoginUser");
        Method listUserByPageMethod = UserController.class.getMethod("listUserByPage", UserQueryRequest.class);
        Method getTeacherMethod = UserController.class.getMethod("getTeacher", TeacherQueryRequest.class);
        Method getUserByIdMethod = UserController.class.getMethod("getUserById", long.class);
        Method getUserVOByIdMethod = UserController.class.getMethod("getUserVOById", long.class);

        // 2. 调用反射检查 UserController 自身声明的字段
        Field[] declaredFields = UserController.class.getDeclaredFields();
        boolean hasTransactionOrMapper = false;
        for (Field field : declaredFields) {
            String typeName = field.getType().getSimpleName();
            if (typeName.contains("TransactionTemplate") || typeName.endsWith("Mapper")) {
                hasTransactionOrMapper = true;
            }
        }

        // 3. 断言 USR-001 ~ USR-008 限流资源名正确且 UserController 无任何 TransactionTemplate 与 Mapper 字段
        assertFalse(hasTransactionOrMapper);
        assertEquals("user.manage.add", addUserMethod.getAnnotation(SentinelRateLimit.class).resource());
        assertEquals("user.manage.delete", deleteUserMethod.getAnnotation(SentinelRateLimit.class).resource());
        assertEquals("user.manage.update", updateUserMethod.getAnnotation(SentinelRateLimit.class).resource());
        assertEquals("user.query.current", getLoginUserMethod.getAnnotation(SentinelRateLimit.class).resource());
        assertEquals("user.query.page", listUserByPageMethod.getAnnotation(SentinelRateLimit.class).resource());
        assertEquals("user.query.teacher-list", getTeacherMethod.getAnnotation(SentinelRateLimit.class).resource());
        assertEquals("user.query.by-id", getUserByIdMethod.getAnnotation(SentinelRateLimit.class).resource());
        assertEquals("user.query.vo-by-id", getUserVOByIdMethod.getAnnotation(SentinelRateLimit.class).resource());
    }

    // 场景：测试 SelectionPolicyController、SystemController 与 UserController 各端点正确委托服务层
    @Test
    void controllers_shouldDelegateToApplicationServices() {
        // 1. 准备测试数据
        SetDeptConfigRequest deptConfigRequest = new SetDeptConfigRequest();
        UserAddRequest userAddRequest = new UserAddRequest();
        DeleteRequest deleteRequest = new DeleteRequest();
        UserUpdateRequest updateRequest = new UserUpdateRequest();
        UserQueryRequest queryRequest = new UserQueryRequest();
        TeacherQueryRequest teacherQueryRequest = new TeacherQueryRequest();

        when(selectionPolicyService.getCrossTopicStatus()).thenReturn(true);
        when(selectionPolicyService.setCrossTopicStatus(true)).thenReturn("跨系选题功能已开启");
        when(selectionPolicyService.getViewTopicStatus()).thenReturn(true);
        when(selectionPolicyService.setViewTopicStatus(false)).thenReturn("禁止学生查看选题");
        when(selectionPolicyService.getSwitchSingleChoiceStatus()).thenReturn(false);
        when(selectionPolicyService.setSwitchSingleChoiceStatus(true)).thenReturn("当前单选模式切换为学生单选模式");
        when(selectionPolicyService.getTopicLock()).thenReturn(new TopicLockVO());
        when(selectionPolicyService.setTopicLock(false, null)).thenReturn("当前是否退选加锁为允许退选题目");
        when(selectionPolicyService.getDeptConfig()).thenReturn(new DeptConfigVO());
        when(selectionPolicyService.setDeptConfig(deptConfigRequest)).thenReturn(true);
        when(selectionPolicyService.delDeptConfig()).thenReturn(true);
        when(selectionPolicyService.getSystemInfo()).thenReturn(new TheSystemInfoVO());

        when(userApplicationService.addUser(userAddRequest)).thenReturn(new BaseResponse<>(0, "成功；临时密码（仅显示一次）：Abc12345", 10L));
        when(userApplicationService.deleteUser(deleteRequest)).thenReturn(true);
        when(userApplicationService.updateUser(updateRequest)).thenReturn(true);
        when(userApplicationService.getLoginUser()).thenReturn(new LoginUserVO());
        when(userApplicationService.listUserByPage(queryRequest)).thenReturn(new Page<User>());
        when(userApplicationService.getTeacher(teacherQueryRequest)).thenReturn(Collections.singletonList(new TeacherVO()));
        when(userApplicationService.getUserById(5L)).thenReturn(new User());
        when(userApplicationService.getUserVOById(5L)).thenReturn(new UserVO());

        // 2. 调用控制器方法
        assertTrue(selectionPolicyController.getCrossTopicStatus().getData());
        assertEquals("跨系选题功能已开启", selectionPolicyController.setCrossTopicStatus(true).getData());
        assertTrue(selectionPolicyController.getViewTopicStatus().getData());
        assertEquals("禁止学生查看选题", selectionPolicyController.setViewTopicStatus(false).getData());
        assertFalse(selectionPolicyController.getSwitchSingleChoiceStatus().getData());
        assertEquals("当前单选模式切换为学生单选模式", selectionPolicyController.setSwitchSingleChoiceStatus(true).getData());
        assertNotNull(selectionPolicyController.getTopicLock().getData());
        assertEquals("当前是否退选加锁为允许退选题目", selectionPolicyController.setTopicLock(false, null).getData());
        assertNotNull(selectionPolicyController.getDeptConfig().getData());
        assertTrue(selectionPolicyController.setDeptConfig(deptConfigRequest).getData());
        assertTrue(selectionPolicyController.delDeptConfig().getData());
        assertNotNull(systemController.test());
        assertNotNull(systemController.getSystemInfo().getData());

        BaseResponse<Long> addRes = userController.addUser(userAddRequest);
        BaseResponse<Boolean> delRes = userController.deleteUser(deleteRequest);
        BaseResponse<Boolean> updRes = userController.updateUser(updateRequest);
        BaseResponse<LoginUserVO> loginRes = userController.getLoginUser();
        BaseResponse<Page<User>> pageRes = userController.listUserByPage(queryRequest);
        BaseResponse<List<TeacherVO>> teacherRes = userController.getTeacher(teacherQueryRequest);
        BaseResponse<User> byIdRes = userController.getUserById(5L);
        BaseResponse<UserVO> voByIdRes = userController.getUserVOById(5L);

        // 3. 断言返回结果与委托调用正确
        assertEquals(10L, addRes.getData());
        assertTrue(delRes.getData());
        assertTrue(updRes.getData());
        assertNotNull(loginRes.getData());
        assertNotNull(pageRes.getData());
        assertEquals(1, teacherRes.getData().size());
        assertNotNull(byIdRes.getData());
        assertNotNull(voByIdRes.getData());
        verify(userApplicationService).addUser(userAddRequest);
        verify(userApplicationService).deleteUser(deleteRequest);
        verify(userApplicationService).updateUser(updateRequest);
    }

}
