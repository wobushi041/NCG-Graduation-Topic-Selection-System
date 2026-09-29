package cn.edu.nfu.topicselection.service.impl;

import cn.edu.nfu.topicselection.constant.TopicConstant;
import cn.edu.nfu.topicselection.exception.BusinessException;
import cn.edu.nfu.topicselection.manager.redis.RedisManager;
import cn.edu.nfu.topicselection.mapper.StudentTopicSelectionMapper;
import cn.edu.nfu.topicselection.mapper.TopicMapper;
import cn.edu.nfu.topicselection.mapper.UserMapper;
import cn.edu.nfu.topicselection.model.entity.User;
import cn.edu.nfu.topicselection.model.request.policy.SetCollegeConfigRequest;
import cn.edu.nfu.topicselection.model.request.user.DeleteRequest;
import cn.edu.nfu.topicselection.model.request.user.UserAddRequest;
import cn.edu.nfu.topicselection.model.request.user.UserQueryRequest;
import cn.edu.nfu.topicselection.model.request.user.UserUpdateRequest;
import cn.edu.nfu.topicselection.service.CollegeService;
import cn.edu.nfu.topicselection.service.PasswordService;
import cn.edu.nfu.topicselection.service.MajorService;
import cn.edu.nfu.topicselection.service.StudentTopicSelectionService;
import cn.edu.nfu.topicselection.service.SwitchService;
import cn.edu.nfu.topicselection.service.TopicService;
import cn.edu.nfu.topicselection.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * 用户应用服务与选题策略服务实现类单元测试
 *
 * @author wobushi041
 */
@ExtendWith(MockitoExtension.class)
class UserAndPolicyServiceTest {

    /**
     * 模拟用户服务依赖
     */
    @Mock
    private UserService userService;

    /**
     * 模拟密码服务依赖
     */
    @Mock
    private PasswordService passwordService;

    /**
     * 模拟用户持久层依赖
     */
    @Mock
    private UserMapper userMapper;

    /**
     * 模拟专业服务依赖
     */
    @Mock
    private MajorService majorService;

    /**
     * 模拟课题服务依赖
     */
    @Mock
    private TopicService topicService;

    /**
     * 模拟课题持久层依赖
     */
    @Mock
    private TopicMapper topicMapper;

    /**
     * 模拟学生选题关联服务依赖
     */
    @Mock
    private StudentTopicSelectionService studentTopicSelectionService;

    /**
     * 模拟学生选题关联持久层依赖
     */
    @Mock
    private StudentTopicSelectionMapper studentTopicSelectionMapper;

    /**
     * 模拟开关服务依赖
     */
    @Mock
    private SwitchService switchService;

    /**
     * 模拟 Redis 管理依赖
     */
    @Mock
    private RedisManager redisManager;

    /**
     * 模拟系部服务依赖
     */
    @Mock
    private CollegeService collegeService;

    /**
     * 待测用户应用服务实现实例
     */
    @InjectMocks
    private UserApplicationServiceImpl userApplicationService;

    /**
     * 待测选题开关与策略服务实现实例
     */
    @InjectMocks
    private SelectionPolicyServiceImpl selectionPolicyService;

    // 场景：测试 UserApplicationServiceImpl 与 SelectionPolicyServiceImpl 的精细事务边界
    @Test
    void transactionBoundaries_shouldOnlyAnnotateDatabaseWriteMethods() throws Exception {
        // 1. 准备测试数据
        Method addUserMethod = UserApplicationServiceImpl.class.getMethod("addUser", UserAddRequest.class);
        Method deleteUserMethod = UserApplicationServiceImpl.class.getMethod("deleteUser", DeleteRequest.class);
        Method updateUserMethod = UserApplicationServiceImpl.class.getMethod("updateUser", UserUpdateRequest.class);
        Method getLoginUserMethod = UserApplicationServiceImpl.class.getMethod("getLoginUser");
        Method listUserByPageMethod = UserApplicationServiceImpl.class.getMethod("listUserByPage", UserQueryRequest.class);
        Method setCrossTopicMethod = SelectionPolicyServiceImpl.class.getMethod("setCrossTopicStatus", boolean.class);
        Method getSystemInfoMethod = SelectionPolicyServiceImpl.class.getMethod("getSystemInfo");

        // 2. 调用反射检查注解
        Transactional userClassTx = UserApplicationServiceImpl.class.getAnnotation(Transactional.class);
        Transactional policyClassTx = SelectionPolicyServiceImpl.class.getAnnotation(Transactional.class);

        // 3. 断言仅 addUser、deleteUser、updateUser 具有 @Transactional，其余查询与策略方法均无事务注解
        assertNull(userClassTx);
        assertNull(policyClassTx);
        assertNotNull(addUserMethod.getAnnotation(Transactional.class));
        assertNotNull(deleteUserMethod.getAnnotation(Transactional.class));
        assertNotNull(updateUserMethod.getAnnotation(Transactional.class));
        assertNull(getLoginUserMethod.getAnnotation(Transactional.class));
        assertNull(listUserByPageMethod.getAnnotation(Transactional.class));
        assertNull(setCrossTopicMethod.getAnnotation(Transactional.class));
        assertNull(getSystemInfoMethod.getAnnotation(Transactional.class));
    }

    // 场景：测试删除超级管理员（id = 1）时抛出非法操作异常
    @Test
    void deleteUser_shouldThrowWhenTargetIsSuperAdmin() {
        // 1. 准备测试数据
        DeleteRequest request = new DeleteRequest();
        request.setUserAccount("admin");
        User superAdmin = new User();
        superAdmin.setId(1L);
        superAdmin.setUserAccount("admin");
        when(userService.userIsExist("admin")).thenReturn(superAdmin);

        // 2. 调用 deleteUser 方法并捕获异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> userApplicationService.deleteUser(request));

        // 3. 断言错误文案与存量逻辑一致
        assertEquals("无法删除超级管理员", exception.getMessage());
    }

    // 场景：测试未开启跨系开关时配置系部跨选规则抛出非法操作异常
    @Test
    void setCollegeConfig_shouldThrowWhenCrossTopicSwitchDisabled() {
        // 1. 准备测试数据
        SetCollegeConfigRequest request = new SetCollegeConfigRequest();
        when(switchService.isEnabled(TopicConstant.CROSS_TOPIC_SWITCH)).thenReturn(false);

        // 2. 调用 setCollegeConfig 方法并捕获异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> selectionPolicyService.setCollegeConfig(request));

        // 3. 断言错误文案与存量逻辑一致
        assertEquals("请先开启跨学院开关后再配置选题规则", exception.getMessage());
    }

    // 场景：测试设置退选加锁时间戳早于或等于当前时间时抛出参数异常
    @Test
    void setTopicLock_shouldThrowWhenTimestampIsNotFuture() {
        // 1. 准备测试数据
        String pastTimestamp = "1000";

        // 2. 调用 setTopicLock 方法并捕获异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> selectionPolicyService.setTopicLock(true, pastTimestamp));

        // 3. 断言错误文案与存量逻辑一致
        assertEquals("加锁时间必须晚于当前时间", exception.getMessage());
    }

}
