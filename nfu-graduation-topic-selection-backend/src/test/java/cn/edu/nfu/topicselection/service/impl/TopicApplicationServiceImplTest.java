package cn.edu.nfu.topicselection.service.impl;

import cn.edu.nfu.topicselection.exception.BusinessException;
import cn.edu.nfu.topicselection.manager.ai.AIManager;
import cn.edu.nfu.topicselection.manager.ai.AIResult;
import cn.edu.nfu.topicselection.manager.redis.RedisManager;
import cn.edu.nfu.topicselection.mapper.TopicMapper;
import cn.edu.nfu.topicselection.mapper.UserMapper;
import cn.edu.nfu.topicselection.model.entity.Topic;
import cn.edu.nfu.topicselection.model.entity.TopicGroup;
import cn.edu.nfu.topicselection.model.entity.User;
import cn.edu.nfu.topicselection.model.enums.TopicStatusEnum;
import cn.edu.nfu.topicselection.model.enums.UserRoleEnum;
import cn.edu.nfu.topicselection.model.request.topic.AddTopicRequest;
import cn.edu.nfu.topicselection.model.request.topic.CheckTopicRequest;
import cn.edu.nfu.topicselection.model.request.topic.DeleteTopicRequest;
import cn.edu.nfu.topicselection.model.request.topic.GetTopicReviewLevelRequest;
import cn.edu.nfu.topicselection.model.request.topic.SetTeacherTopicAmountRequest;
import cn.edu.nfu.topicselection.model.request.topic.UnSetTimeRequest;
import cn.edu.nfu.topicselection.model.request.topic.UpdateTopicRequest;
import cn.edu.nfu.topicselection.model.vo.UnpublishTopicResultVO;
import cn.edu.nfu.topicselection.service.MailService;
import cn.edu.nfu.topicselection.service.StudentTopicSelectionService;
import cn.edu.nfu.topicselection.service.TeacherGroupService;
import cn.edu.nfu.topicselection.service.TopicService;
import cn.edu.nfu.topicselection.service.TopicGroupService;
import cn.edu.nfu.topicselection.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 课题全生命周期应用服务实现单元测试
 *
 * @author wobushi041
 */
@ExtendWith(MockitoExtension.class)
class TopicApplicationServiceImplTest {

    /**
     * 待测课题应用服务实现
     */
    private TopicApplicationServiceImpl topicApplicationService;

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
    private StudentTopicSelectionService studentTopicSelectionService;

    /**
     * 模拟教师选题组服务
     */
    @Mock
    private TeacherGroupService teacherGroupService;

    /**
     * 模拟选题组服务
     */
    @Mock
    private TopicGroupService topicGroupService;

    /**
     * 模拟邮箱通知服务
     */
    @Mock
    private MailService mailService;

    /**
     * 模拟 Redis 缓存管理器
     */
    @Mock
    private RedisManager redisManager;

    /**
     * 模拟 AI 查重管理器
     */
    @Mock
    private AIManager aiManager;

    @BeforeEach
    void setUp() {
        topicApplicationService = new TopicApplicationServiceImpl(
                userMapper,
                topicMapper,
                userService,
                topicService,
                studentTopicSelectionService,
                teacherGroupService,
                topicGroupService,
                mailService,
                redisManager,
                aiManager
        );
    }

    // 场景：测试教师添加课题时使用组选题额度校验且不再扣减旧的全局额度
    @Test
    void addTopicUsesGroupQuotaWithoutDecrementingLegacyTopicAmount() {
        // 1. 准备教师账号与添加课题请求数据
        User teacher = teacherUser(10L, "teacher-01", "王老师", "计算机系", 3);
        AddTopicRequest request = new AddTopicRequest();
        request.setTopic("基于深度学习的图像识别研究");
        request.setType("研究型");
        request.setDescription("研究卷积神经网络在工业缺陷检测中的应用");
        request.setRequirement("熟悉 Python 与 PyTorch 框架");
        request.setSurplusQuantity(2);
        request.setTopicGroupId(1L);

        TopicGroup topicGroup = new TopicGroup();
        topicGroup.setId(1L);
        topicGroup.setCollegeId(1L);

        when(topicService.getOne(any())).thenReturn(null);
        when(userService.userGetCurrentLoginUser()).thenReturn(teacher);
        when(userMapper.selectByIdForUpdate(teacher.getId())).thenReturn(teacher);
        when(userService.userIsTeacher(teacher)).thenReturn(true);
        when(topicGroupService.getById(1L)).thenReturn(topicGroup);
        when(topicService.save(any(Topic.class))).thenAnswer(invocation -> {
            Topic saved = invocation.getArgument(0);
            saved.setId(99L);
            return true;
        });
        // 2. 调用添加课题方法
        Long topicId = topicApplicationService.addTopic(request);

        // 3. 断言返回新课题 ID、调用组选题额度校验且不更新旧额度字段
        assertEquals(99L, topicId);
        assertEquals(3, teacher.getTopicAmount());
        verify(teacherGroupService).validate("teacher-01", 1L, null);
        verify(userService, never()).updateById(teacher);
    }

    // 场景：测试教师删除课题时按先锁教师再锁题目的顺序执行且不再回补旧的全局额度
    @Test
    void deleteTopicLocksTeacherBeforeTopicWithoutRestoringLegacyTopicAmount() {
        // 1. 准备教师与归属该教师的课题数据
        User teacher = teacherUser(10L, "teacher-01", "王老师", "计算机系", 2);
        Topic topic = new Topic();
        topic.setId(55L);
        topic.setTeacherAccount(teacher.getUserAccount());
        topic.setTeacherName(teacher.getUserName());

        DeleteTopicRequest request = new DeleteTopicRequest();
        request.setId(55L);

        when(userService.userGetCurrentLoginUser()).thenReturn(teacher);
        when(userMapper.selectByIdForUpdate(teacher.getId())).thenReturn(teacher);
        when(topicMapper.selectByIdForUpdate(55L)).thenReturn(topic);
        when(topicService.removeById(55L)).thenReturn(true);
        // 2. 调用删除课题方法
        Boolean result = topicApplicationService.deleteTopic(request);

        // 3. 断言删除成功、加锁顺序为先教师后课题且旧额度字段保持不变
        assertTrue(result);
        assertEquals(2, teacher.getTopicAmount());
        InOrder lockOrder = inOrder(userMapper, topicMapper);
        lockOrder.verify(userMapper).selectByIdForUpdate(teacher.getId());
        lockOrder.verify(topicMapper).selectByIdForUpdate(55L);
        verify(studentTopicSelectionService).remove(any());
        verify(userService, never()).updateById(teacher);
    }

    // 场景：测试管理员设置教师出题上限小于已发布题目数量时被拦截
    @Test
    void setTeacherTopicAmountRejectsQuotaSmallerThanCurrentTopicCount() {
        // 1. 准备已出 5 道题目的教师与设置上限为 3 的请求
        User teacher = teacherUser(10L, "teacher-01", "王老师", "计算机系", 5);
        SetTeacherTopicAmountRequest request = new SetTeacherTopicAmountRequest();
        request.setTeacherId(10L);
        request.setTopicAmount(3);

        when(userService.getById(10L)).thenReturn(teacher);
        when(topicService.count(any())).thenReturn(5L);

        // 2. 调用设置教师题目上限方法
        // 3. 断言抛出参数错误异常且未更新教师记录
        assertThrows(BusinessException.class, () -> topicApplicationService.setTeacherTopicAmount(request));
        verify(userService, never()).updateById(any(User.class));
    }

    // 场景：测试教师修改未发布课题时同步更新总容量并重新计算剩余余量进入待审核状态
    @Test
    void updateTopicUpdatesStudentCapacityAndResetsReviewStatus() {
        // 1. 准备教师、被打回课题与新的可接收学生数量（初始容量 1，无人已确认选题）
        User teacher = teacherUser(10L, "teacher-01", "王老师", "计算机系", 3);
        Topic topic = new Topic();
        topic.setId(66L);
        topic.setTopic("校园预约系统");
        topic.setTeacherAccount(teacher.getUserAccount());
        topic.setTopicGroupId(1L);
        topic.setSurplusQuantity(1);
        topic.setCapacity(1);
        topic.setStatus(TopicStatusEnum.REJECTED.getCode());

        UpdateTopicRequest request = new UpdateTopicRequest();
        request.setTopicName("校园预约系统");
        request.setType("软件系统");
        request.setDescription("实现校园场地预约与审批流程");
        request.setRequirement("掌握 Java 与 React 开发");
        request.setTopicGroupId(1L);
        request.setSurplusQuantity(3);

        TopicGroup topicGroup = new TopicGroup();
        topicGroup.setId(1L);
        topicGroup.setCollegeId(1L);

        when(userService.userGetCurrentLoginUser()).thenReturn(teacher);
        when(topicService.getOne(any())).thenReturn(topic);
        when(userMapper.selectByIdForUpdate(teacher.getId())).thenReturn(teacher);
        when(topicMapper.selectByIdForUpdate(topic.getId())).thenReturn(topic);
        when(topicGroupService.getById(1L)).thenReturn(topicGroup);
        when(topicService.updateById(topic)).thenReturn(true);

        // 2. 调用修改课题方法
        String result = topicApplicationService.updateTopic(request);

        // 3. 断言总容量更新为 3，无人占用时剩余余量同为 3，状态重置为待审核
        assertEquals("更新成功", result);
        assertEquals(3, topic.getCapacity());
        assertEquals(3, topic.getSurplusQuantity());
        assertEquals(TopicStatusEnum.PENDING_REVIEW.getCode(), topic.getStatus());
        verify(teacherGroupService).validate("teacher-01", 1L, 66L);
        verify(topicService).updateById(topic);
    }

    // 场景：测试批量取消发布时成功处理无占用题目并返回被学生占用题目的跳过原因
    @Test
    void unsetTimeByIdReturnsCancelledAndSkippedTopicsForMixedBatch() {
        // 1. 准备一个无学生占用题目、一个已有学生选题的题目与批量请求
        Topic cancellableTopic = publishedTopic(11L, "边缘计算实验平台", "王老师");
        Topic occupiedTopic = publishedTopic(12L, "校园物联网平台", "李老师");
        UnSetTimeRequest request = new UnSetTimeRequest();
        request.setTopicIds(Arrays.asList(11L, 12L));

        when(topicMapper.selectByIdForUpdate(11L)).thenReturn(cancellableTopic);
        when(topicMapper.selectByIdForUpdate(12L)).thenReturn(occupiedTopic);
        when(studentTopicSelectionService.count(any())).thenReturn(0L, 2L);
        when(topicService.update(any())).thenReturn(true);

        // 2. 调用批量取消发布方法
        UnpublishTopicResultVO result = topicApplicationService.unsetTimeById(request);

        // 3. 断言无占用题目取消成功，占用题目保留并返回可读原因
        assertEquals(Collections.singletonList(11L), result.getCancelledTopicIds());
        assertEquals(1, result.getSkippedTopics().size());
        assertEquals(12L, result.getSkippedTopics().get(0).getTopicId());
        assertEquals("校园物联网平台", result.getSkippedTopics().get(0).getTopicName());
        assertEquals(2L, result.getSkippedTopics().get(0).getActiveSelectionCount());
        assertEquals("题目已有 2 名学生预选或确认选择，无法取消发布", result.getSkippedTopics().get(0).getReason());
        verify(topicService).update(any());
    }

    // 场景：测试全部题目均被学生占用时仅返回跳过结果且不更新课题状态
    @Test
    void unsetTimeByIdDoesNotUpdateWhenAllTopicsAreOccupied() {
        // 1. 准备一个已有学生选题的已发布题目与取消发布请求
        Topic occupiedTopic = publishedTopic(21L, "智能排课系统", "周老师");
        UnSetTimeRequest request = new UnSetTimeRequest();
        request.setTopicIds(Collections.singletonList(21L));

        when(topicMapper.selectByIdForUpdate(21L)).thenReturn(occupiedTopic);
        when(studentTopicSelectionService.count(any())).thenReturn(3L);

        // 2. 调用批量取消发布方法
        UnpublishTopicResultVO result = topicApplicationService.unsetTimeById(request);

        // 3. 断言没有题目被取消发布、返回占用原因且不执行状态更新
        assertTrue(result.getCancelledTopicIds().isEmpty());
        assertEquals(1, result.getSkippedTopics().size());
        assertEquals(21L, result.getSkippedTopics().get(0).getTopicId());
        assertEquals(3L, result.getSkippedTopics().get(0).getActiveSelectionCount());
        verify(topicService, never()).update(any());
    }


    // 场景：测试系主任审核退回课题时记录退回理由并向出题教师发送通知邮件
    @Test
    void checkTopicSendsRejectionMailWhenCollegeRejectsTopic() {
        // 1. 准备同系部同选题组的系主任、待审核课题与出题教师邮箱数据
        User collegeUser = new User();
        collegeUser.setId(20L);
        collegeUser.setUserAccount("college-01");
        collegeUser.setUserName("李主任");
        collegeUser.setCollegeId(1L);
        collegeUser.setMajorId(1L);
        collegeUser.setTopicGroupId(1L);
        collegeUser.setUserRole(UserRoleEnum.TOPIC_LEADER.getCode());

        Topic topic = new Topic();
        topic.setId(88L);
        topic.setTopicGroupId(1L);
        topic.setTeacherAccount("teacher-01");
        topic.setStatus(TopicStatusEnum.PENDING_REVIEW.getCode());

        User teacher = teacherUser(10L, "teacher-01", "王老师", "计算机系", 3);
        teacher.setEmail("teacher01@example.com");

        CheckTopicRequest request = new CheckTopicRequest();
        request.setId(88L);
        request.setStatus(TopicStatusEnum.REJECTED.getCode());
        request.setReason("题目范围过大，请细化技术指标");

        when(userService.userGetCurrentLoginUser()).thenReturn(collegeUser);
        when(topicMapper.selectByIdForUpdate(88L)).thenReturn(topic);
        when(topicService.updateById(topic)).thenReturn(true);
        when(userService.getOne(any())).thenReturn(teacher);

        // 2. 调用审核课题方法
        Boolean result = topicApplicationService.checkTopic(request);

        // 3. 断言课题状态更新为退回、填写系主任姓名并触发退回理由邮件
        assertTrue(result);
        assertEquals(TopicStatusEnum.REJECTED.getCode(), topic.getStatus());
        assertEquals("题目范围过大，请细化技术指标", topic.getReason());
        verify(mailService).sendReasonMail("teacher01@example.com", "广州南方学院毕设选题管理系统", "题目范围过大，请细化技术指标");
    }

    // 场景：测试课题 AI 审核等级检测受 Redis 每日 30 次限流保护且正常调用 AI 服务
    @Test
    void getTopicReviewLevelEnforcesRedisDailyRateLimitAndCallsAiManager() {
        // 1. 准备教师登录态与 AI 查重请求数据
        User teacher = teacherUser(10L, "teacher-01", "王老师", "计算机系", 3);
        GetTopicReviewLevelRequest request = new GetTopicReviewLevelRequest();
        request.setTopic("微服务架构下的毕业选题系统");
        request.setDescription("实现基于 Spring Boot 与 Sa-Token 的毕业设计选题管理系统");

        AIResult expectedAiResult = new AIResult();
        when(userService.userGetCurrentLoginUser()).thenReturn(teacher);
        when(redisManager.tryAcquire("ai-review-rate:10", 30, 86400)).thenReturn(true);
        when(aiManager.sendAi(anyString(), anyString())).thenReturn(expectedAiResult);

        // 2. 调用获取题目审核等级方法
        AIResult actual = topicApplicationService.getTopicReviewLevel(request);

        // 3. 断言成功返回 AI 检测结果；当 Redis 限流拒绝时抛出业务限流异常
        assertSame(expectedAiResult, actual);
        verify(redisManager).tryAcquire(eq("ai-review-rate:10"), eq(30), eq(86400L));

        when(redisManager.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(false);
        assertThrows(BusinessException.class, () -> topicApplicationService.getTopicReviewLevel(request));
    }

    /**
     * 构造测试教师用户实体
     *
     * @param id          用户 ID
     * @param account     教师工号
     * @param name        教师姓名
     * @param college        所属系部
     * @param topicAmount 剩余出题配额
     * @return 教师用户实体
     */
    private static User teacherUser(Long id, String account, String name, String college, int topicAmount) {
        User user = new User();
        user.setId(id);
        user.setUserAccount(account);
        user.setUserName(name);
        user.setCollegeId(1L);
        user.setUserRole(UserRoleEnum.TEACHER.getCode());
        user.setTopicAmount(topicAmount);
        return user;
    }

    /**
     * 构造已发布课题实体
     *
     * @param id          课题 ID
     * @param topicName   课题名称
     * @param teacherName 指导教师姓名
     * @return 已发布课题实体
     */
    private static Topic publishedTopic(Long id, String topicName, String teacherName) {
        Topic topic = new Topic();
        topic.setId(id);
        topic.setTopic(topicName);
        topic.setTeacherName(teacherName);
        topic.setStatus(TopicStatusEnum.PUBLISHED.getCode());
        return topic;
    }

}
