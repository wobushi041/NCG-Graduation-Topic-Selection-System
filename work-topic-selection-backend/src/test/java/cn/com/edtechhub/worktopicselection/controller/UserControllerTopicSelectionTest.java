package cn.com.edtechhub.worktopicselection.controller;

import cn.com.edtechhub.worktopicselection.constant.TopicConstant;
import cn.com.edtechhub.worktopicselection.exception.BusinessException;
import cn.com.edtechhub.worktopicselection.manager.redis.RedisManager;
import cn.com.edtechhub.worktopicselection.mapper.StudentTopicSelectionMapper;
import cn.com.edtechhub.worktopicselection.mapper.TopicMapper;
import cn.com.edtechhub.worktopicselection.mapper.UserMapper;
import cn.com.edtechhub.worktopicselection.model.entity.Project;
import cn.com.edtechhub.worktopicselection.model.entity.StudentTopicSelection;
import cn.com.edtechhub.worktopicselection.model.entity.Topic;
import cn.com.edtechhub.worktopicselection.model.entity.User;
import cn.com.edtechhub.worktopicselection.model.enums.StudentTopicSelectionStatusEnum;
import cn.com.edtechhub.worktopicselection.model.enums.TopicStatusEnum;
import cn.com.edtechhub.worktopicselection.model.enums.UserRoleEnum;
import cn.com.edtechhub.worktopicselection.model.request.selection.SelectTopicByIdRequest;
import cn.com.edtechhub.worktopicselection.service.MailService;
import cn.com.edtechhub.worktopicselection.service.ProjectService;
import cn.com.edtechhub.worktopicselection.service.StudentTopicSelectionService;
import cn.com.edtechhub.worktopicselection.service.SwitchService;
import cn.com.edtechhub.worktopicselection.service.TeacherGroupService;
import cn.com.edtechhub.worktopicselection.service.TopicService;
import cn.com.edtechhub.worktopicselection.service.UserService;
import cn.com.edtechhub.worktopicselection.service.impl.TopicApplicationServiceImpl;
import cn.com.edtechhub.worktopicselection.service.impl.TopicSelectionApplicationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 选题写用例与课题状态流转测试
 *
 * @author wobushi041
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTopicSelectionTest {

    /**
     * 待测课题全生命周期应用服务实现
     */
    private TopicApplicationServiceImpl topicApplicationService;

    /**
     * 待测选题写用例服务实现
     */
    private TopicSelectionApplicationServiceImpl selectionApplicationService;

    /**
     * 模拟用户数据访问层
     */
    @Mock
    private UserMapper userMapper;

    /**
     * 模拟课题数据访问层
     */
    @Mock
    private TopicMapper topicMapper;

    /**
     * 模拟学生选题关联数据访问层
     */
    @Mock
    private StudentTopicSelectionMapper selectionMapper;

    /**
     * 模拟用户服务
     */
    @Mock
    private UserService userService;

    /**
     * 模拟专业服务
     */
    @Mock
    private ProjectService projectService;

    /**
     * 模拟课题服务
     */
    @Mock
    private TopicService topicService;

    /**
     * 模拟学生选题关联服务
     */
    @Mock
    private StudentTopicSelectionService selectionService;

    /**
     * 模拟教师选题组服务
     */
    @Mock
    private TeacherGroupService teacherGroupService;

    /**
     * 模拟系统开关服务
     */
    @Mock
    private SwitchService switchService;

    /**
     * 模拟 Redis 缓存管理器
     */
    @Mock
    private RedisManager redisManager;

    /**
     * 模拟邮箱通知服务
     */
    @Mock
    private MailService mailService;

    @BeforeEach
    void setUp() {
        topicApplicationService = new TopicApplicationServiceImpl(
                userMapper,
                topicMapper,
                userService,
                topicService,
                selectionService,
                teacherGroupService,
                projectService,
                mailService,
                redisManager,
                null
        );
        selectionApplicationService = new TopicSelectionApplicationServiceImpl(
                userMapper,
                topicMapper,
                selectionMapper,
                userService,
                projectService,
                topicService,
                selectionService,
                switchService,
                redisManager,
                mailService
        );
    }

    // 场景：测试学生可以选择相同选题组配置下的课题
    @Test
    void studentCanSelectTopicFromTheSameConfiguredGroup() {
        // 1. 准备同选题组的学生、专业和课题测试数据
        User student = user(1L, "student-1", "学生甲", "计算机系", UserRoleEnum.STUDENT);
        student.setProject("计算机科学与技术");
        Topic topic = publishedTopic(10L, 1);
        topic.setTopicGroup("第一组");
        Project project = new Project();
        project.setProjectName(student.getProject());
        project.setGroupName("第一组");

        when(projectService.getOne(any())).thenReturn(project);

        // 2. 调用选题组校验方法
        // 3. 断言校验通过且不抛出业务异常
        assertDoesNotThrow(() -> selectionApplicationService.validateStudentTopicGroup(student, topic));
    }

    // 场景：测试学生预选不同选题组的课题时被拦截
    @Test
    void preselectionRejectsTopicFromAnotherConfiguredGroup() {
        // 1. 准备不同选题组的学生与课题测试数据
        User student = user(1L, "student-1", "学生甲", "计算机系", UserRoleEnum.STUDENT);
        student.setProject("计算机科学与技术");
        Topic topic = publishedTopic(10L, 1);
        topic.setTopicGroup("第二组");
        Project project = new Project();
        project.setProjectName(student.getProject());
        project.setGroupName("第一组");

        when(userService.userGetCurrentLoginUser()).thenReturn(student);
        when(userMapper.selectByIdForUpdate(student.getId())).thenReturn(student);
        when(userService.userIsStudent(student)).thenReturn(true);
        when(topicMapper.selectByIdForUpdate(topic.getId())).thenReturn(topic);
        when(projectService.getOne(any())).thenReturn(project);

        SelectTopicByIdRequest request = new SelectTopicByIdRequest();
        request.setId(topic.getId());
        request.setStatus(StudentTopicSelectionStatusEnum.EN_PRESELECT.getCode());

        // 2. 调用预选课题方法
        // 3. 断言抛出业务异常且未保存预选记录
        assertThrows(BusinessException.class, () -> selectionApplicationService.preselectTopicById(request));
        verify(selectionService, never()).save(any(StudentTopicSelection.class));
    }

    // 场景：测试学生确认不同选题组的最终选题时被拦截
    @Test
    void finalSelectionRejectsTopicFromAnotherConfiguredGroup() {
        // 1. 准备不同选题组的学生与课题测试数据
        User student = user(1L, "student-1", "学生甲", "计算机系", UserRoleEnum.STUDENT);
        student.setProject("计算机科学与技术");
        Topic topic = publishedTopic(10L, 1);
        topic.setTopicGroup("第二组");
        Project project = new Project();
        project.setProjectName(student.getProject());
        project.setGroupName("第一组");

        when(userService.userGetCurrentLoginUser()).thenReturn(student);
        when(userMapper.selectByIdForUpdate(student.getId())).thenReturn(student);
        when(userService.userIsStudent(student)).thenReturn(true);
        when(topicMapper.selectByIdForUpdate(topic.getId())).thenReturn(topic);
        when(projectService.getOne(any())).thenReturn(project);

        SelectTopicByIdRequest request = new SelectTopicByIdRequest();
        request.setId(topic.getId());
        request.setStatus(StudentTopicSelectionStatusEnum.EN_SELECT.getCode());

        // 2. 调用确认最终选题方法
        // 3. 断言抛出业务异常且未更新选题记录
        assertThrows(BusinessException.class, () -> selectionApplicationService.selectTopicById(request));
        verify(selectionService, never()).updateById(any(StudentTopicSelection.class));
    }

    // 场景：测试课题归属权与审核状态流转绑定到操作人身份
    @Test
    void topicOwnershipAndReviewTransitionsAreBoundToActor() {
        // 1. 准备不同系部与角色的教师、主任和课题数据
        User teacher = user(1L, "teacher-1", "张老师", "计算机系", UserRoleEnum.TEACHER);
        User otherTeacher = user(2L, "teacher-2", "张老师", "计算机系", UserRoleEnum.TEACHER);
        User dept = user(3L, "dept-1", "王主任", "计算机系", UserRoleEnum.DEPT);
        User otherDept = user(4L, "dept-2", "赵主任", "外语系", UserRoleEnum.DEPT);
        Topic topic = publishedTopic(10L, 1);
        topic.setTeacherName(teacher.getUserName());
        topic.setTeacherAccount(teacher.getUserAccount());
        topic.setDeptName(teacher.getDept());

        // 2. 调用课题归属权与状态流转判断方法
        assertTrue(selectionApplicationService.isTopicOwner(teacher, topic));
        assertFalse(selectionApplicationService.isTopicOwner(otherTeacher, topic));

        dept.setProject("计科");
        Project project = new Project();
        project.setGroupName("第一组");
        when(projectService.getOne(any())).thenReturn(project);
        topic.setTopicGroup("第一组");
        topic.setStatus(TopicStatusEnum.PENDING_REVIEW.getCode());

        // 3. 断言课题审核状态转换权限符合角色与分组约束
        assertTrue(topicApplicationService.isAllowedTopicStatusTransition(dept, topic, TopicStatusEnum.NOT_PUBLISHED));
        assertTrue(topicApplicationService.isAllowedTopicStatusTransition(dept, topic, TopicStatusEnum.REJECTED));
        assertFalse(topicApplicationService.isAllowedTopicStatusTransition(otherDept, topic, TopicStatusEnum.REJECTED));
        assertFalse(topicApplicationService.isAllowedTopicStatusTransition(teacher, topic, TopicStatusEnum.NOT_PUBLISHED));
        topic.setTopicGroup("第二组");
        assertFalse(topicApplicationService.isAllowedTopicStatusTransition(dept, topic, TopicStatusEnum.NOT_PUBLISHED));
        topic.setTopicGroup("第一组");

        topic.setStatus(TopicStatusEnum.REJECTED.getCode());
        assertTrue(topicApplicationService.isAllowedTopicStatusTransition(teacher, topic, TopicStatusEnum.PENDING_REVIEW));
        assertFalse(topicApplicationService.isAllowedTopicStatusTransition(otherTeacher, topic, TopicStatusEnum.PENDING_REVIEW));
    }

    // 场景：测试移除预选记录不会错误增加课题剩余余量
    @Test
    void removingPreselectionDoesNotInflateCapacity() {
        // 1. 准备包含预选与最终选题状态的课题记录
        Topic topic = publishedTopic(10L, 2);
        topic.setSelectAmount(1);
        StudentTopicSelection preselection = selection(20L, "student-1", topic.getId(), StudentTopicSelectionStatusEnum.EN_PRESELECT);

        // 2. 恢复预选记录与最终选题记录的课题计数
        selectionApplicationService.restoreTopicCounters(topic, preselection);
        assertEquals(2, topic.getSurplusQuantity());
        assertEquals(0, topic.getSelectAmount());

        StudentTopicSelection finalSelection = selection(21L, "student-1", topic.getId(), StudentTopicSelectionStatusEnum.EN_SELECT);
        selectionApplicationService.restoreTopicCounters(topic, finalSelection);

        // 3. 断言只有最终选题退回时才恢复剩余可选余量
        assertEquals(3, topic.getSurplusQuantity());
        assertEquals(0, topic.getSelectAmount());
    }

    // 场景：测试确认最终选题时按学生到课题到选题记录的固定顺序加悲观锁并扣减一次余量
    @Test
    void confirmSelectionLocksStudentBeforeTopicAndConsumesCapacityOnce() {
        // 1. 准备已预选课题的学生与开放中的课题数据
        User student = user(1L, "student-1", "学生甲", "计算机系", UserRoleEnum.STUDENT);
        Topic topic = publishedTopic(10L, 1);
        StudentTopicSelection preselection = selection(20L, student.getUserAccount(), topic.getId(), StudentTopicSelectionStatusEnum.EN_PRESELECT);

        when(userMapper.selectByIdForUpdate(student.getId())).thenReturn(student);
        when(userService.userIsStudent(student)).thenReturn(true);
        when(topicMapper.selectByIdForUpdate(topic.getId())).thenReturn(topic);
        when(switchService.isEnabled(TopicConstant.SWITCH_SINGLE_CHOICE)).thenReturn(true);
        when(switchService.isEnabled(TopicConstant.CROSS_TOPIC_SWITCH)).thenReturn(false);
        when(selectionMapper.selectByUserForUpdate(student.getUserAccount())).thenReturn(Collections.singletonList(preselection));
        when(selectionService.updateById(preselection)).thenReturn(true);
        when(topicService.updateById(topic)).thenReturn(true);

        // 2. 调用确认学生最终选题方法
        Long selectionId = selectionApplicationService.confirmStudentSelection(student, topic.getId());

        // 3. 断言悲观锁顺序与课题余量扣减结果正确
        assertEquals(preselection.getId(), selectionId);
        assertEquals(StudentTopicSelectionStatusEnum.EN_SELECT.getCode(), preselection.getStatus());
        assertEquals(0, topic.getSurplusQuantity());
        verify(selectionService, never()).save(any(StudentTopicSelection.class));
        InOrder lockOrder = inOrder(userMapper, topicMapper, selectionMapper);
        lockOrder.verify(userMapper).selectByIdForUpdate(student.getId());
        lockOrder.verify(topicMapper).selectByIdForUpdate(topic.getId());
        lockOrder.verify(selectionMapper).selectByUserForUpdate(student.getUserAccount());
    }

    // 场景：测试已存在最终选题记录的学生无法再次确认第二个课题
    @Test
    void finalSelectionOnAnotherTopicBlocksSecondChoice() {
        // 1. 准备同时拥有预选与另一门已确认课题的学生数据
        User student = user(1L, "student-1", "学生甲", "计算机系", UserRoleEnum.STUDENT);
        Topic topic = publishedTopic(10L, 1);
        StudentTopicSelection currentPreselection = selection(20L, student.getUserAccount(), topic.getId(), StudentTopicSelectionStatusEnum.EN_PRESELECT);
        StudentTopicSelection existingFinal = selection(21L, student.getUserAccount(), 11L, StudentTopicSelectionStatusEnum.EN_SELECT);

        when(userMapper.selectByIdForUpdate(student.getId())).thenReturn(student);
        when(userService.userIsStudent(student)).thenReturn(true);
        when(topicMapper.selectByIdForUpdate(topic.getId())).thenReturn(topic);
        when(switchService.isEnabled(TopicConstant.SWITCH_SINGLE_CHOICE)).thenReturn(true);
        when(switchService.isEnabled(TopicConstant.CROSS_TOPIC_SWITCH)).thenReturn(false);
        when(selectionMapper.selectByUserForUpdate(student.getUserAccount())).thenReturn(Arrays.asList(currentPreselection, existingFinal));

        // 2. 调用确认学生最终选题方法
        // 3. 断言抛出异常且课题余量未被扣减
        assertThrows(BusinessException.class, () -> selectionApplicationService.confirmStudentSelection(student, topic.getId()));
        assertEquals(1, topic.getSurplusQuantity());
        verify(selectionService, never()).updateById(any(StudentTopicSelection.class));
        verify(topicService, never()).updateById(any(Topic.class));
    }

    // 场景：测试教师双选确认学生时复用已有的预选记录并按固定顺序加悲观锁
    @Test
    void teacherAssignmentReusesExistingPreselection() {
        // 1. 准备教师、学生及已预选课题数据
        User teacher = user(1L, "teacher-1", "张老师", "计算机系", UserRoleEnum.TEACHER);
        User student = user(2L, "student-1", "学生甲", "计算机系", UserRoleEnum.STUDENT);
        Topic topic = publishedTopic(10L, 1);
        topic.setTeacherName(teacher.getUserName());
        topic.setTeacherAccount(teacher.getUserAccount());
        topic.setDeptName(teacher.getDept());
        StudentTopicSelection preselection = selection(20L, student.getUserAccount(), topic.getId(), StudentTopicSelectionStatusEnum.EN_PRESELECT);

        when(userMapper.selectByIdForUpdate(student.getId())).thenReturn(student);
        when(userService.userIsStudent(student)).thenReturn(true);
        when(topicMapper.selectByIdForUpdate(topic.getId())).thenReturn(topic);
        when(selectionMapper.selectByUserForUpdate(student.getUserAccount())).thenReturn(Collections.singletonList(preselection));
        when(selectionService.updateById(preselection)).thenReturn(true);
        when(topicService.updateById(topic)).thenReturn(true);

        // 2. 调用教师确认学生选题方法
        selectionApplicationService.assignStudentSelection(teacher, student, topic.getId());

        // 3. 断言预选记录状态升级为最终确认且加锁顺序正确
        assertEquals(StudentTopicSelectionStatusEnum.EN_SELECT.getCode(), preselection.getStatus());
        assertEquals(0, topic.getSurplusQuantity());
        assertEquals(0, topic.getSelectAmount());
        verify(selectionService, never()).save(any(StudentTopicSelection.class));
        InOrder lockOrder = inOrder(userMapper, topicMapper, selectionMapper);
        lockOrder.verify(userMapper).selectByIdForUpdate(student.getId());
        lockOrder.verify(topicMapper).selectByIdForUpdate(topic.getId());
        lockOrder.verify(selectionMapper).selectByUserForUpdate(student.getUserAccount());
    }

    /**
     * 构造测试用户实体
     *
     * @param id      用户 ID
     * @param account 用户账号
     * @param name    用户姓名
     * @param dept    所属系部
     * @param role    用户角色枚举
     * @return 测试用户实体
     */
    private static User user(Long id, String account, String name, String dept, UserRoleEnum role) {
        User user = new User();
        user.setId(id);
        user.setUserAccount(account);
        user.setUserName(name);
        user.setDept(dept);
        user.setUserRole(role.getCode());
        return user;
    }

    /**
     * 构造已发布且处于开放时间窗口内的测试课题实体
     *
     * @param id      课题 ID
     * @param surplus 剩余可选余量
     * @return 测试课题实体
     */
    private static Topic publishedTopic(Long id, int surplus) {
        Topic topic = new Topic();
        topic.setId(id);
        topic.setStatus(TopicStatusEnum.PUBLISHED.getCode());
        topic.setDeptName("计算机系");
        topic.setSurplusQuantity(surplus);
        topic.setSelectAmount(0);
        topic.setStartTime(new Date(System.currentTimeMillis() - 60_000));
        topic.setEndTime(new Date(System.currentTimeMillis() + 60_000));
        return topic;
    }

    /**
     * 构造测试学生选题关联记录
     *
     * @param id      记录 ID
     * @param account 学生账号
     * @param topicId 课题 ID
     * @param status  选题状态枚举
     * @return 学生选题关联记录
     */
    private static StudentTopicSelection selection(
            Long id,
            String account,
            Long topicId,
            StudentTopicSelectionStatusEnum status
    ) {
        StudentTopicSelection selection = new StudentTopicSelection();
        selection.setId(id);
        selection.setUserAccount(account);
        selection.setTopicId(topicId);
        selection.setStatus(status.getCode());
        return selection;
    }

}
