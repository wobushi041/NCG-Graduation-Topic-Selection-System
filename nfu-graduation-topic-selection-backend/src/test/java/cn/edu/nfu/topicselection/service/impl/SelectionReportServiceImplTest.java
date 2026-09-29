package cn.edu.nfu.topicselection.service.impl;

import cn.edu.nfu.topicselection.constant.TopicConstant;
import cn.edu.nfu.topicselection.exception.BusinessException;
import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import cn.edu.nfu.topicselection.model.entity.College;
import cn.edu.nfu.topicselection.model.entity.StudentTopicSelection;
import cn.edu.nfu.topicselection.model.entity.Major;
import cn.edu.nfu.topicselection.model.entity.Topic;
import cn.edu.nfu.topicselection.model.entity.User;
import cn.edu.nfu.topicselection.model.enums.UserRoleEnum;
import cn.edu.nfu.topicselection.model.request.topic.TopicQueryByAdminRequest;
import cn.edu.nfu.topicselection.model.request.topic.TopicQueryRequest;
import cn.edu.nfu.topicselection.model.request.user.TopicLeaderQueryRequest;
import cn.edu.nfu.topicselection.model.request.user.GetUserListRequest;
import cn.edu.nfu.topicselection.model.request.user.UserQueryRequest;
import cn.edu.nfu.topicselection.model.vo.TopicLeaderVO;
import cn.edu.nfu.topicselection.model.vo.SituationVO;
import cn.edu.nfu.topicselection.model.vo.UserNameVO;
import cn.edu.nfu.topicselection.model.vo.UserVO;
import cn.edu.nfu.topicselection.service.StudentTopicSelectionService;
import cn.edu.nfu.topicselection.service.CollegeService;
import cn.edu.nfu.topicselection.service.MajorService;
import cn.edu.nfu.topicselection.service.SwitchService;
import cn.edu.nfu.topicselection.service.TeacherGroupService;
import cn.edu.nfu.topicselection.service.TopicService;
import cn.edu.nfu.topicselection.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * SelectionReportServiceImpl 单元测试
 *
 * @author wobushi041
 */
@ExtendWith(MockitoExtension.class)
class SelectionReportServiceImplTest {

    /**
     * 模拟用户服务
     */
    @Mock
    private UserService userService;

    /**
     * 模拟选题服务
     */
    @Mock
    private TopicService topicService;

    /**
     * 模拟专业服务
     */
    @Mock
    private MajorService majorService;

    /**
     * 模拟学院服务
     */
    @Mock
    private CollegeService collegeService;

    /**
     * 模拟教师选题组额度服务
     */
    @Mock
    private TeacherGroupService teacherGroupService;

    /**
     * 模拟学生选题关联服务
     */
    @Mock
    private StudentTopicSelectionService studentTopicSelectionService;

    /**
     * 模拟开关服务
     */
    @Mock
    private SwitchService switchService;

    /**
     * 被测只读查询与统计服务实现
     */
    private SelectionReportServiceImpl selectionReportService;

    /**
     * 初始化测试环境
     */
    @BeforeEach
    void setUp() {
        selectionReportService = new SelectionReportServiceImpl(
                userService,
                topicService,
                majorService,
                collegeService,
                teacherGroupService,
                studentTopicSelectionService,
                switchService
        );
    }

    // 场景：测试 SelectionReportServiceImpl 满足 ARCH-05 架构红线（类与方法均无 @Transactional 注解）
    @Test
    void selectionReportServiceImpl_shouldHaveZeroTransactionalAnnotations() {
        // 1. 准备测试数据并获取类与声明方法
        Transactional classTransactional = SelectionReportServiceImpl.class.getAnnotation(Transactional.class);
        Method[] methods = SelectionReportServiceImpl.class.getDeclaredMethods();

        // 2. 检查类级与方法级是否存在 @Transactional
        Assertions.assertNull(classTransactional);
        for (Method method : methods) {
            Assertions.assertNull(method.getAnnotation(Transactional.class));
        }

        // 3. 断言只读查询服务完全无写事务代理开销
        Assertions.assertTrue(methods.length >= 8);
    }

    // 场景：测试 getTopicList 在系主任角色下按系部与专业选题组过滤分页查询
    @Test
    @SuppressWarnings("unchecked")
    void getTopicList_collegeRole_shouldFilterByDepartmentAndGroup() {
        // 1. 准备测试数据
        TopicQueryRequest request = new TopicQueryRequest();
        request.setCurrent(1);
        request.setPageSize(10);
        User collegeUser = new User();
        collegeUser.setUserRole(UserRoleEnum.TOPIC_LEADER.getCode());
        collegeUser.setCollegeId(1L);
        collegeUser.setMajorId(1L);
        collegeUser.setTopicGroupId(1L);
        Page<Topic> expectedPage = new Page<>(1, 10, 1);
        Mockito.when(userService.userGetCurrentLoginUser()).thenReturn(collegeUser);
        Mockito.when(topicService.getQueryWrapper(request)).thenReturn(new QueryWrapper<>());
        Mockito.when(topicService.page(ArgumentMatchers.any(Page.class), ArgumentMatchers.any(QueryWrapper.class))).thenReturn(expectedPage);

        // 2. 调用 getTopicList 方法
        Page<Topic> result = selectionReportService.getTopicList(request);

        // 3. 断言返回分页结果正确
        Assertions.assertSame(expectedPage, result);
    }

    // 场景：测试 getTopicList 在学生角色且未开放查看题目开关时抛出 NOT_FOUND_ERROR 异常
    @Test
    void getTopicList_studentRoleWhenViewSwitchDisabled_shouldThrowNotFoundError() {
        // 1. 准备测试数据
        TopicQueryRequest request = new TopicQueryRequest();
        request.setCurrent(1);
        request.setPageSize(10);
        User studentUser = new User();
        studentUser.setUserRole(UserRoleEnum.STUDENT.getCode());
        studentUser.setCollegeId(1L);
        Mockito.when(userService.userGetCurrentLoginUser()).thenReturn(studentUser);
        Mockito.when(topicService.getQueryWrapper(request)).thenReturn(new QueryWrapper<>());
        Mockito.when(switchService.isEnabled(TopicConstant.VIEW_TOPIC_SWITCH)).thenReturn(false);

        // 2. 调用 getTopicList 方法并捕获业务异常
        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> selectionReportService.getTopicList(request)
        );

        // 3. 断言错误码与提示文案保持不变
        Assertions.assertEquals(CodeBindMessageEnums.NOT_FOUND_ERROR, ex.getCodeBindMessageEnums());
        Assertions.assertEquals("当前时间学生无法查看选题, 请等待系统开放", ex.getMessage());
    }

    // 场景：测试 getSelectTopicSituation 准确汇总总学生数、已选人数与未选人数
    @Test
    @SuppressWarnings("unchecked")
    void getSelectTopicSituation_shouldReturnCorrectStudentCounts() {
        // 1. 准备测试数据
        User adminUser = new User();
        adminUser.setUserRole(UserRoleEnum.ADMIN.getCode());
        User student1 = new User();
        student1.setUserAccount("stu01");
        User student2 = new User();
        student2.setUserAccount("stu02");
        Mockito.when(userService.userGetCurrentLoginUser()).thenReturn(adminUser);
        Mockito.when(userService.userIsTopicLeader(adminUser)).thenReturn(false);
        Mockito.when(userService.userIsAdmin(adminUser)).thenReturn(true);
        Mockito.when(userService.count(ArgumentMatchers.any(QueryWrapper.class))).thenReturn(2L);
        Mockito.when(userService.list(ArgumentMatchers.any(QueryWrapper.class))).thenReturn(Arrays.asList(student1, student2));
        Mockito.when(studentTopicSelectionService.count(ArgumentMatchers.any(QueryWrapper.class)))
                .thenReturn(1L)
                .thenReturn(0L);

        // 2. 调用 getSelectTopicSituation 方法
        SituationVO situationVO = selectionReportService.getSelectTopicSituation();

        // 3. 断言总人数、已选人数与未选人数计算正确
        Assertions.assertEquals(2, situationVO.getAmount());
        Assertions.assertEquals(1, situationVO.getSelectAmount());
        Assertions.assertEquals(1, situationVO.getUnselectAmount());
    }

    // 场景：测试 getTeacher 汇总系部教师题目余量与已选数量并执行内存分页
    @Test
    @SuppressWarnings("unchecked")
    void getTeacher_shouldAggregateTopicCountsAndPaginateInMemory() {
        // 1. 准备测试数据
        TopicLeaderQueryRequest request = new TopicLeaderQueryRequest();
        request.setCurrent(1);
        request.setPageSize(10);
        request.setTeacherName("张老师");
        User teacherUser = new User();
        teacherUser.setUserAccount("t001");
        teacherUser.setUserName("张老师");
        teacherUser.setCollegeId(1L);
        Topic topic = new Topic();
        topic.setSelectAmount(2);
        topic.setSurplusQuantity(3);
        College college = new College();
        college.setId(1L);
        college.setCollegeName("人工智能学院");
        Mockito.when(userService.userGetCurrentLoginUser()).thenReturn(teacherUser);
        Mockito.when(userService.userIsStudent(teacherUser)).thenReturn(false);
        Mockito.when(switchService.isEnabled(TopicConstant.CROSS_TOPIC_SWITCH)).thenReturn(true);
        Mockito.when(userService.list(ArgumentMatchers.any(QueryWrapper.class))).thenReturn(Collections.singletonList(teacherUser));
        Mockito.when(collegeService.listByIds(ArgumentMatchers.anyCollection())).thenReturn(Collections.singletonList(college));
        Mockito.when(topicService.count(ArgumentMatchers.any(QueryWrapper.class))).thenReturn(1L);
        Mockito.when(topicService.list(ArgumentMatchers.any(QueryWrapper.class))).thenReturn(Collections.singletonList(topic));

        // 2. 调用 getTeacher 方法
        Page<TopicLeaderVO> page = selectionReportService.getTeacher(request);

        // 3. 断言统计字段与分页结果正确
        Assertions.assertEquals(1L, page.getTotal());
        Assertions.assertEquals(1, page.getRecords().size());
        Assertions.assertEquals("张老师", page.getRecords().get(0).getTeacherName());
        Assertions.assertEquals("人工智能学院", page.getRecords().get(0).getCollegeName());
        Assertions.assertEquals(2, page.getRecords().get(0).getSelectAmount());
        Assertions.assertEquals(3, page.getRecords().get(0).getSurplusQuantity());
    }

    // 场景：测试 getUnSelectTopicStudentList 过滤排除已选题学生并返回同系部未选题学生列表
    @Test
    @SuppressWarnings("unchecked")
    void getUnSelectTopicStudentList_shouldFilterOutSelectedStudents() {
        // 1. 准备测试数据
        User collegeUser = new User();
        collegeUser.setTopicGroupId(1L);
        Major major = new Major();
        major.setId(10L);
        User student1 = new User();
        student1.setUserAccount("stu01");
        User student2 = new User();
        student2.setUserAccount("stu02");
        StudentTopicSelection selected = new StudentTopicSelection();
        selected.setUserAccount("stu01");
        Mockito.when(userService.userGetCurrentLoginUser()).thenReturn(collegeUser);
        Mockito.when(majorService.list(ArgumentMatchers.any(QueryWrapper.class)))
                .thenReturn(Collections.singletonList(major));
        Mockito.when(userService.list(ArgumentMatchers.any(QueryWrapper.class))).thenReturn(Arrays.asList(student1, student2));
        Mockito.when(studentTopicSelectionService.list(ArgumentMatchers.any(QueryWrapper.class))).thenReturn(Collections.singletonList(selected));

        // 2. 调用 getUnSelectTopicStudentList 方法
        List<User> unselected = selectionReportService.getUnSelectTopicStudentList();

        // 3. 断言仅返回未选题学生 stu02
        Assertions.assertEquals(1, unselected.size());
        Assertions.assertEquals("stu02", unselected.get(0).getUserAccount());
    }

    // 场景：测试 listUserVOByPage 校验每页大小上限为 20 并转换返回 UserVO 分页数据
    @Test
    @SuppressWarnings("unchecked")
    void listUserVOByPage_shouldEnforceMaxSizeTwentyAndReturnUserVOPage() {
        // 1. 准备测试数据
        UserQueryRequest invalidRequest = new UserQueryRequest();
        invalidRequest.setCurrent(1);
        invalidRequest.setPageSize(21);
        UserQueryRequest validRequest = new UserQueryRequest();
        validRequest.setCurrent(1);
        validRequest.setPageSize(10);
        Page<User> userPage = new Page<>(1, 10, 1);
        userPage.setRecords(Collections.singletonList(new User()));
        UserVO userVO = new UserVO();
        userVO.setUserName("测试用户");
        Mockito.when(userService.getQueryWrapper(validRequest)).thenReturn(new QueryWrapper<>());
        Mockito.when(userService.page(ArgumentMatchers.any(Page.class), ArgumentMatchers.any(QueryWrapper.class))).thenReturn(userPage);
        Mockito.when(userService.getUserVO(userPage.getRecords())).thenReturn(Collections.singletonList(userVO));

        // 2. 校验超限请求抛出异常并调用合法请求
        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> selectionReportService.listUserVOByPage(invalidRequest)
        );
        Page<UserVO> result = selectionReportService.listUserVOByPage(validRequest);

        // 3. 断言错误提示与正常返回结果正确
        Assertions.assertEquals("页大小必须在 1 到 20 之间", ex.getMessage());
        Assertions.assertEquals(1L, result.getTotal());
        Assertions.assertEquals("测试用户", result.getRecords().get(0).getUserName());
    }

    // 场景：测试 getUserList 与 getTopicListByAdmin 正常返回结果
    @Test
    @SuppressWarnings("unchecked")
    void getUserListAndGetTopicListByAdmin_shouldReturnExpectedData() {
        // 1. 准备测试数据
        GetUserListRequest userListRequest = new GetUserListRequest();
        userListRequest.setUserRole(UserRoleEnum.TEACHER.getCode());
        User teacher = new User();
        teacher.setUserName("李老师");
        TopicQueryByAdminRequest adminTopicRequest = new TopicQueryByAdminRequest();
        adminTopicRequest.setCurrent(1);
        adminTopicRequest.setPageSize(10);
        Page<Topic> expectedTopicPage = new Page<>(1, 10, 1);
        Mockito.when(userService.list(ArgumentMatchers.any(QueryWrapper.class))).thenReturn(Collections.singletonList(teacher));
        Mockito.when(topicService.getTopicQueryByAdminWrapper(adminTopicRequest)).thenReturn(new QueryWrapper<>());
        Mockito.when(topicService.page(ArgumentMatchers.any(Page.class), ArgumentMatchers.any(QueryWrapper.class))).thenReturn(expectedTopicPage);

        // 2. 调用 getUserList 与 getTopicListByAdmin 方法
        List<UserNameVO> userNameVOList = selectionReportService.getUserList(userListRequest);
        Page<Topic> topicPage = selectionReportService.getTopicListByAdmin(adminTopicRequest);

        // 3. 断言返回结果正确
        Assertions.assertEquals(1, userNameVOList.size());
        Assertions.assertEquals("李老师", userNameVOList.get(0).getUserName());
        Assertions.assertSame(expectedTopicPage, topicPage);
    }

    // 场景：测试 getTeacherByAdmin 查询本系部存在待审核题目的教师并执行内存分页
    @Test
    @SuppressWarnings("unchecked")
    void getTeacherByAdmin_shouldReturnPendingReviewTeachersInDepartment() {
        // 1. 准备测试数据
        TopicLeaderQueryRequest request = new TopicLeaderQueryRequest();
        request.setCurrent(1);
        request.setPageSize(10);
        User collegeUser = new User();
        collegeUser.setTopicGroupId(1L);
        User teacherUser = new User();
        teacherUser.setUserAccount("t001");
        teacherUser.setUserName("王老师");
        Topic pendingTopic = new Topic();
        pendingTopic.setSelectAmount(0);
        pendingTopic.setSurplusQuantity(5);
        Mockito.when(userService.userGetCurrentLoginUser()).thenReturn(collegeUser);
        Mockito.when(teacherGroupService.teacherAccountsForGroup(1L))
                .thenReturn(Collections.singletonList("t001"));
        Mockito.when(userService.list(ArgumentMatchers.any(QueryWrapper.class))).thenReturn(Collections.singletonList(teacherUser));
        Mockito.when(topicService.count(ArgumentMatchers.any(QueryWrapper.class))).thenReturn(1L);
        Mockito.when(topicService.list(ArgumentMatchers.any(QueryWrapper.class))).thenReturn(Collections.singletonList(pendingTopic));

        // 2. 调用 getTeacherByAdmin 方法
        Page<TopicLeaderVO> page = selectionReportService.getTeacherByAdmin(request);

        // 3. 断言返回待审核题目教师分页数据正确
        Assertions.assertEquals(1L, page.getTotal());
        Assertions.assertEquals("王老师", page.getRecords().get(0).getTeacherName());
        Assertions.assertEquals(5, page.getRecords().get(0).getSurplusQuantity());
    }

}
