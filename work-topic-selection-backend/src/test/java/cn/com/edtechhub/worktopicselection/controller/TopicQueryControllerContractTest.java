package cn.com.edtechhub.worktopicselection.controller;

import cn.com.edtechhub.worktopicselection.annotation.SentinelRateLimit;
import cn.com.edtechhub.worktopicselection.exception.CodeBindMessageEnums;
import cn.com.edtechhub.worktopicselection.model.entity.Topic;
import cn.com.edtechhub.worktopicselection.model.entity.User;
import cn.com.edtechhub.worktopicselection.model.request.topic.TopicQueryByAdminRequest;
import cn.com.edtechhub.worktopicselection.model.request.topic.TopicQueryRequest;
import cn.com.edtechhub.worktopicselection.model.request.user.DeptTeacherQueryRequest;
import cn.com.edtechhub.worktopicselection.model.request.user.GetUserListRequest;
import cn.com.edtechhub.worktopicselection.model.request.user.UserQueryRequest;
import cn.com.edtechhub.worktopicselection.model.vo.DeptTeacherVO;
import cn.com.edtechhub.worktopicselection.model.vo.SituationVO;
import cn.com.edtechhub.worktopicselection.model.vo.UserNameVO;
import cn.com.edtechhub.worktopicselection.model.vo.UserVO;
import cn.com.edtechhub.worktopicselection.response.BaseResponse;
import cn.com.edtechhub.worktopicselection.service.SelectionReportService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * TopicQueryController 路由契约与注解守护测试
 *
 * @author wobushi041
 */
@ExtendWith(MockitoExtension.class)
class TopicQueryControllerContractTest {

    /**
     * 模拟选题题目与统计报表只读查询服务
     */
    @Mock
    private SelectionReportService selectionReportService;

    /**
     * 被测选题题目与统计报表只读查询控制层
     */
    private TopicQueryController topicQueryController;

    /**
     * 初始化测试环境
     */
    @BeforeEach
    void setUp() {
        topicQueryController = new TopicQueryController(selectionReportService);
    }

    // 场景：测试 TopicQueryController 类级路由前缀与 8 个查询统计接口的路由、限流及鉴权注解契约
    @Test
    void topicQueryController_shouldPreserveRoutesSentinelAndSaTokenAnnotations() throws Exception {
        // 1. 准备测试数据并获取类级注解
        RequestMapping classMapping = TopicQueryController.class.getAnnotation(RequestMapping.class);

        // 2. 校验类级路径前缀并获取 8 个接口方法反射对象
        Assertions.assertNotNull(classMapping);
        Assertions.assertArrayEquals(new String[]{"/user"}, classMapping.value());
        Method getTopicListMethod = TopicQueryController.class.getMethod("getTopicList", TopicQueryRequest.class);
        Method getSituationMethod = TopicQueryController.class.getMethod("getSelectTopicSituation");
        Method getTeacherMethod = TopicQueryController.class.getMethod("getTeacher", DeptTeacherQueryRequest.class);
        Method getUnselectedMethod = TopicQueryController.class.getMethod("getUnSelectTopicStudentList");
        Method getTopicAdminMethod = TopicQueryController.class.getMethod("getTopicListByAdmin", TopicQueryByAdminRequest.class);
        Method listUserVOMethod = TopicQueryController.class.getMethod("listUserVOByPage", UserQueryRequest.class);
        Method getUserListMethod = TopicQueryController.class.getMethod("getUserList", GetUserListRequest.class);
        Method getTeacherByAdminMethod = TopicQueryController.class.getMethod("getTeacherByAdmin", DeptTeacherQueryRequest.class);

        // 3. 断言 8 个接口的 HTTP 路径、Sentinel 资源名与 Sa-Token 角色鉴权完全符合契约
        assertPostEndpoint(getTopicListMethod, "/get/topic/page", "query.topic.page", null);
        assertPostEndpoint(getSituationMethod, "/get/select/topic/situation", "query.selection.situation", new String[]{"admin", "dept"});
        assertPostEndpoint(getTeacherMethod, "/get/dept/teacher", "query.dept.teacher", null);
        assertPostEndpoint(getUnselectedMethod, "/get/unselect/topic/student/list", "query.selection.unselected-students", new String[]{"dept"});
        assertPostEndpoint(getTopicAdminMethod, "/get/topic/list/by/admin", "query.topic.admin-page", new String[]{"admin"});
        assertPostEndpoint(listUserVOMethod, "/list/page/vo", "query.user.vo-page", new String[]{"admin"});
        assertPostEndpoint(getUserListMethod, "/get/user/list", "query.user.name-list", new String[]{"admin"});
        assertPostEndpoint(getTeacherByAdminMethod, "/get/dept/teacher/by/admin", "query.dept.pending-teacher", new String[]{"dept"});
    }

    // 场景：测试 TopicQueryController 8 个方法委托调用 SelectionReportService 并封装统一响应
    @Test
    void topicQueryController_shouldDelegateAllEightEndpointsToSelectionReportService() {
        // 1. 准备测试数据与模拟返回值
        TopicQueryRequest topicQueryRequest = new TopicQueryRequest();
        DeptTeacherQueryRequest deptTeacherQueryRequest = new DeptTeacherQueryRequest();
        TopicQueryByAdminRequest topicQueryByAdminRequest = new TopicQueryByAdminRequest();
        UserQueryRequest userQueryRequest = new UserQueryRequest();
        GetUserListRequest getUserListRequest = new GetUserListRequest();
        Page<Topic> topicPage = new Page<>(1, 10);
        SituationVO situationVO = new SituationVO();
        Page<DeptTeacherVO> teacherPage = new Page<>(1, 10);
        List<User> unselectedStudents = Collections.singletonList(new User());
        Page<UserVO> userVOPage = new Page<>(1, 10);
        List<UserNameVO> userNameVOList = Collections.singletonList(new UserNameVO());
        Mockito.when(selectionReportService.getTopicList(topicQueryRequest)).thenReturn(topicPage);
        Mockito.when(selectionReportService.getSelectTopicSituation()).thenReturn(situationVO);
        Mockito.when(selectionReportService.getTeacher(deptTeacherQueryRequest)).thenReturn(teacherPage);
        Mockito.when(selectionReportService.getUnSelectTopicStudentList()).thenReturn(unselectedStudents);
        Mockito.when(selectionReportService.getTopicListByAdmin(topicQueryByAdminRequest)).thenReturn(topicPage);
        Mockito.when(selectionReportService.listUserVOByPage(userQueryRequest)).thenReturn(userVOPage);
        Mockito.when(selectionReportService.getUserList(getUserListRequest)).thenReturn(userNameVOList);
        Mockito.when(selectionReportService.getTeacherByAdmin(deptTeacherQueryRequest)).thenReturn(teacherPage);

        // 2. 依次调用 TopicQueryController 的 8 个接口方法
        BaseResponse<Page<Topic>> res1 = topicQueryController.getTopicList(topicQueryRequest);
        BaseResponse<SituationVO> res2 = topicQueryController.getSelectTopicSituation();
        BaseResponse<Page<DeptTeacherVO>> res3 = topicQueryController.getTeacher(deptTeacherQueryRequest);
        BaseResponse<List<User>> res4 = topicQueryController.getUnSelectTopicStudentList();
        BaseResponse<Page<Topic>> res5 = topicQueryController.getTopicListByAdmin(topicQueryByAdminRequest);
        BaseResponse<Page<UserVO>> res6 = topicQueryController.listUserVOByPage(userQueryRequest);
        BaseResponse<List<UserNameVO>> res7 = topicQueryController.getUserList(getUserListRequest);
        BaseResponse<Page<DeptTeacherVO>> res8 = topicQueryController.getTeacherByAdmin(deptTeacherQueryRequest);

        // 3. 断言响应体状态码与返回数据正确
        Assertions.assertEquals(CodeBindMessageEnums.SUCCESS.getCode(), res1.getCode());
        Assertions.assertSame(topicPage, res1.getData());
        Assertions.assertEquals(CodeBindMessageEnums.SUCCESS.getCode(), res2.getCode());
        Assertions.assertSame(situationVO, res2.getData());
        Assertions.assertEquals(CodeBindMessageEnums.SUCCESS.getCode(), res3.getCode());
        Assertions.assertSame(teacherPage, res3.getData());
        Assertions.assertEquals(CodeBindMessageEnums.SUCCESS.getCode(), res4.getCode());
        Assertions.assertSame(unselectedStudents, res4.getData());
        Assertions.assertEquals(CodeBindMessageEnums.SUCCESS.getCode(), res5.getCode());
        Assertions.assertSame(topicPage, res5.getData());
        Assertions.assertEquals(CodeBindMessageEnums.SUCCESS.getCode(), res6.getCode());
        Assertions.assertSame(userVOPage, res6.getData());
        Assertions.assertEquals(CodeBindMessageEnums.SUCCESS.getCode(), res7.getCode());
        Assertions.assertSame(userNameVOList, res7.getData());
        Assertions.assertEquals(CodeBindMessageEnums.SUCCESS.getCode(), res8.getCode());
        Assertions.assertSame(teacherPage, res8.getData());
    }

    // 场景：测试 UserController 在 Wave 4 迁移完成后仅保留 UserApplicationService 单一依赖且无手工 Sentinel 残留
    @Test
    void userController_shouldRetainOnlyUserApplicationServiceDependency() {
        // 1. 准备测试数据并获取 UserController 声明字段列表
        Field[] declaredFields = UserController.class.getDeclaredFields();

        // 2. 筛选非静态实例字段
        List<Field> instanceFields = Arrays.asList(declaredFields);

        // 3. 断言 UserController 仅包含 userApplicationService 单一依赖
        Assertions.assertEquals(1, instanceFields.size());
        Assertions.assertEquals("userApplicationService", instanceFields.get(0).getName());
    }

    /**
     * 校验 POST 接口方法的路径、Sentinel 限流注解与 Sa-Token 鉴权注解
     *
     * @param method        目标反射方法
     * @param expectedPath  期望的 POST 路径
     * @param expectedRes   期望的 Sentinel 资源名
     * @param expectedRoles 期望的 SaCheckRole 角色数组（为 null 表示仅校验登录）
     */
    private void assertPostEndpoint(Method method, String expectedPath, String expectedRes, String[] expectedRoles) {
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        Assertions.assertNotNull(postMapping);
        Assertions.assertArrayEquals(new String[]{expectedPath}, postMapping.value());

        SentinelRateLimit rateLimit = method.getAnnotation(SentinelRateLimit.class);
        Assertions.assertNotNull(rateLimit);
        Assertions.assertEquals(expectedRes, rateLimit.resource());

        Assertions.assertNotNull(method.getAnnotation(SaCheckLogin.class));
        SaCheckRole saCheckRole = method.getAnnotation(SaCheckRole.class);
        if (expectedRoles == null) {
            Assertions.assertNull(saCheckRole);
        } else {
            Assertions.assertNotNull(saCheckRole);
            Assertions.assertArrayEquals(expectedRoles, saCheckRole.value());
        }
    }

}
