package cn.edu.nfu.topicselection.service;

import cn.edu.nfu.topicselection.exception.BusinessException;
import cn.edu.nfu.topicselection.model.entity.StudentTopicSelection;
import cn.edu.nfu.topicselection.model.entity.Topic;
import cn.edu.nfu.topicselection.model.entity.User;
import cn.edu.nfu.topicselection.model.enums.StudentTopicSelectionStatusEnum;
import cn.edu.nfu.topicselection.model.enums.UserRoleEnum;
import cn.edu.nfu.topicselection.model.request.selection.GetSelectTopicByIdRequest;
import cn.edu.nfu.topicselection.model.request.selection.GetSelectTopicRequest;
import cn.edu.nfu.topicselection.model.request.selection.GetStudentByTopicIdRequest;
import cn.edu.nfu.topicselection.service.impl.TopicSelectionQueryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 学生选题与教师确认读用例服务测试
 *
 * @author wobushi041
 */
@ExtendWith(MockitoExtension.class)
class TopicSelectionQueryServiceTest {

    /**
     * 待测选题读用例服务实现
     */
    private TopicSelectionQueryServiceImpl queryService;

    /**
     * 模拟用户服务
     */
    @Mock
    private UserService userService;

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

    @BeforeEach
    void setUp() {
        queryService = new TopicSelectionQueryServiceImpl(userService, topicService, selectionService);
    }

    // 场景：测试教师查询本人课题下的已选学生列表
    @Test
    void teacherCanQuerySelectedStudentsForOwnedTopic() {
        // 1. 准备归属当前教师的课题与已选学生记录
        User teacher = user(1L, "teacher-1", "张老师", "计算机系", UserRoleEnum.TEACHER);
        User student = user(2L, "student-1", "学生甲", "计算机系", UserRoleEnum.STUDENT);
        Topic topic = new Topic();
        topic.setId(10L);
        topic.setTeacherAccount(teacher.getUserAccount());
        topic.setTeacherName(teacher.getUserName());

        StudentTopicSelection selection = new StudentTopicSelection();
        selection.setTopicId(10L);
        selection.setUserAccount(student.getUserAccount());
        selection.setStatus(StudentTopicSelectionStatusEnum.EN_SELECT.getCode());

        when(userService.userGetCurrentLoginUser()).thenReturn(teacher);
        when(topicService.getById(10L)).thenReturn(topic);
        when(selectionService.list(any())).thenReturn(Collections.singletonList(selection));
        when(userService.getOne(any())).thenReturn(student);

        GetSelectTopicByIdRequest request = new GetSelectTopicByIdRequest();
        request.setId(10L);

        // 2. 调用查询已选学生列表方法
        List<User> students = queryService.getSelectTopicById(request);

        // 3. 断言返回的学生列表与预期一致
        assertEquals(1, students.size());
        assertEquals("student-1", students.get(0).getUserAccount());
    }

    // 场景：测试非课题归属教师按课题 ID 查询学生时被权限拦截
    @Test
    void nonOwnerTeacherCannotQueryStudentsByTopicId() {
        // 1. 准备非本人归属的课题数据
        User otherTeacher = user(2L, "teacher-2", "李老师", "计算机系", UserRoleEnum.TEACHER);
        Topic topic = new Topic();
        topic.setId(10L);
        topic.setTeacherAccount("teacher-1");
        topic.setTeacherName("张老师");

        when(userService.userGetCurrentLoginUser()).thenReturn(otherTeacher);
        when(topicService.getById(10L)).thenReturn(topic);

        GetStudentByTopicIdRequest request = new GetStudentByTopicIdRequest();
        request.setId(10L);

        // 2. 调用按课题 ID 查询学生方法
        // 3. 断言抛出无权限业务异常
        assertThrows(BusinessException.class, () -> queryService.getStudentByTopicId(request));
    }

    // 场景：测试学生查询已预选的课题列表與最终选题确认时间戳
    @Test
    void studentCanQueryPreselectedTopicsAndChoiceTimestamp() {
        // 1. 准备学生的预选记录与最终选题记录
        User student = user(1L, "student-1", "学生甲", "计算机系", UserRoleEnum.STUDENT);
        Topic topic = new Topic();
        topic.setId(10L);
        topic.setTopic("分布式选题系统设计");

        StudentTopicSelection selection = new StudentTopicSelection();
        selection.setTopicId(10L);
        selection.setUserAccount(student.getUserAccount());
        selection.setStatus(StudentTopicSelectionStatusEnum.EN_SELECT.getCode());
        selection.setUpdateTime(new Date(1700000000000L));

        when(userService.userGetCurrentLoginUser()).thenReturn(student);
        when(selectionService.list(any())).thenReturn(Collections.singletonList(selection));
        when(topicService.listByIds(Collections.singletonList(10L))).thenReturn(Collections.singletonList(topic));
        when(selectionService.getOne(any())).thenReturn(selection);

        GetSelectTopicRequest timeRequest = new GetSelectTopicRequest();
        timeRequest.setTopicId(10L);

        // 2. 调用预选课题查询与最终选中时间查询方法
        List<Topic> preselectedTopics = queryService.getPreTopic();
        String choiceTime = queryService.getSelectTopicTime(timeRequest);

        // 3. 断言返回课题列表与秒级时间戳字符串正确
        assertEquals(1, preselectedTopics.size());
        assertEquals(10L, preselectedTopics.get(0).getId());
        assertEquals("1700000000", choiceTime);
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

}
