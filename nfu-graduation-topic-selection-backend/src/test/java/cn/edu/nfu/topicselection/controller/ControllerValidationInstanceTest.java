package cn.edu.nfu.topicselection.controller;

import cn.edu.nfu.topicselection.aop.RequestDtoValidationAspect;
import cn.edu.nfu.topicselection.exception.BusinessException;
import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import cn.edu.nfu.topicselection.model.request.ai.AiSendRequest;
import cn.edu.nfu.topicselection.model.request.organization.CollegeAddRequest;
import cn.edu.nfu.topicselection.model.request.organization.TeacherGroupQuotaUpdateRequest;
import cn.edu.nfu.topicselection.model.request.policy.SetCollegeConfigRequest;
import cn.edu.nfu.topicselection.model.request.topic.AddTopicRequest;
import cn.edu.nfu.topicselection.model.request.user.DeleteRequest;
import cn.edu.nfu.topicselection.model.request.user.UserUpdateRequest;
import cn.edu.nfu.topicselection.response.BaseResponse;
import cn.edu.nfu.topicselection.service.AIApplicationService;
import cn.edu.nfu.topicselection.service.OrganizationApplicationService;
import cn.edu.nfu.topicselection.service.SelectionPolicyService;
import cn.edu.nfu.topicselection.service.TopicApplicationService;
import cn.edu.nfu.topicselection.service.UserApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 控制器接入请求校验切面的代理实例接口测试
 *
 * @author wobushi041
 */
@ExtendWith(MockitoExtension.class)
class ControllerValidationInstanceTest {

    /**
     * 校验器实例
     */
    private Validator validator;

    /**
     * 模拟组织应用服务
     */
    @Mock
    private OrganizationApplicationService organizationApplicationService;

    /**
     * 模拟选题应用服务
     */
    @Mock
    private TopicApplicationService topicApplicationService;

    /**
     * 模拟策略配置服务
     */
    @Mock
    private SelectionPolicyService selectionPolicyService;

    /**
     * 模拟 AI 应用服务
     */
    @Mock
    private AIApplicationService aiApplicationService;

    /**
     * 模拟用户应用服务
     */
    @Mock
    private UserApplicationService userApplicationService;

    /**
     * 组织架构控制器代理实例
     */
    private OrganizationController organizationControllerProxy;

    /**
     * 教师选题组控制器代理实例
     */
    private TeacherGroupController teacherGroupControllerProxy;

    /**
     * 课题管理控制器代理实例
     */
    private TopicController topicControllerProxy;

    /**
     * 策略配置控制器代理实例
     */
    private SelectionPolicyController selectionPolicyControllerProxy;

    /**
     * AI 控制器代理实例
     */
    private AIController aiControllerProxy;

    /**
     * 用户管理控制器代理实例
     */
    private UserController userControllerProxy;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        RequestDtoValidationAspect aspect = new RequestDtoValidationAspect(validator);

        // 为 OrganizationController 创建织入切面的代理实例
        AspectJProxyFactory orgFactory = new AspectJProxyFactory(new OrganizationController(organizationApplicationService));
        orgFactory.addAspect(aspect);
        organizationControllerProxy = orgFactory.getProxy();

        // 为 TeacherGroupController 创建织入切面的代理实例
        AspectJProxyFactory groupFactory = new AspectJProxyFactory(new TeacherGroupController(organizationApplicationService));
        groupFactory.addAspect(aspect);
        teacherGroupControllerProxy = groupFactory.getProxy();

        // 为 TopicController 创建织入切面的代理实例
        AspectJProxyFactory topicFactory = new AspectJProxyFactory(new TopicController(topicApplicationService));
        topicFactory.addAspect(aspect);
        topicControllerProxy = topicFactory.getProxy();

        // 为 SelectionPolicyController 创建织入切面的代理实例
        AspectJProxyFactory policyFactory = new AspectJProxyFactory(new SelectionPolicyController(selectionPolicyService));
        policyFactory.addAspect(aspect);
        selectionPolicyControllerProxy = policyFactory.getProxy();

        // 为 AIController 创建织入切面的代理实例
        AspectJProxyFactory aiFactory = new AspectJProxyFactory(new AIController(aiApplicationService));
        aiFactory.addAspect(aspect);
        aiControllerProxy = aiFactory.getProxy();

        // 为 UserController 创建织入切面的代理实例
        AspectJProxyFactory userFactory = new AspectJProxyFactory(new UserController(userApplicationService));
        userFactory.addAspect(aspect);
        userControllerProxy = userFactory.getProxy();
    }

    // 场景：测试当请求体为 null 时切面直接拦截并快速失败
    @Test
    void nullRequestBody_shouldBeInterceptedByAspectWithParamsError() {
        // 1. 准备 null 请求体调用添加学院接口
        // 2. 调用代理实例并捕获异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> organizationControllerProxy.addCollege(null));

        // 3. 断言抛出统一定义的参数错误且底层服务未被调用
        assertEquals(CodeBindMessageEnums.PARAMS_ERROR, exception.getCodeBindMessageEnums());
        assertEquals("请求体不能为空", exception.getExceptionMessage());
        verify(organizationApplicationService, never()).addCollege(any());
    }

    // 场景：测试添加学院时学院名称为空被切面拦截且底层服务不被调用
    @Test
    void addCollege_shouldInterceptBlankCollegeName() {
        // 1. 准备学院名称为空白的请求体
        CollegeAddRequest request = new CollegeAddRequest();
        request.setCollegeName("   ");

        // 2. 调用代理实例并捕获异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> organizationControllerProxy.addCollege(request));

        // 3. 断言错误文案精确匹配且底层服务未被执行
        assertEquals(CodeBindMessageEnums.PARAMS_ERROR, exception.getCodeBindMessageEnums());
        assertEquals("学院名称不能为空", exception.getExceptionMessage());
        verify(organizationApplicationService, never()).addCollege(any());
    }

    // 场景：测试修改教师选题组额度时超出范围被切面拦截
    @Test
    void updateTeacherGroupQuota_shouldInterceptInvalidMaxTopics() {
        // 1. 准备最大出题数量为 99（允许范围 0~20）的请求体
        TeacherGroupQuotaUpdateRequest request = new TeacherGroupQuotaUpdateRequest();
        request.setTeacherAccount("T001");
        request.setTopicGroupId(1L);
        request.setMaxTopics(99);

        // 2. 调用代理实例并捕获异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> teacherGroupControllerProxy.updateTeacherGroupQuota(request));

        // 3. 断言错误文案精确匹配且底层服务未被执行
        assertEquals(CodeBindMessageEnums.PARAMS_ERROR, exception.getCodeBindMessageEnums());
        assertEquals("最大出题数量必须在 0 到 20 之间", exception.getExceptionMessage());
        verify(organizationApplicationService, never()).updateTeacherGroupQuota(any());
    }

    // 场景：测试配置跨学院策略时空配置列表被切面拦截
    @Test
    void setCollegeConfig_shouldInterceptEmptyCollegeMap() {
        // 1. 准备空的学院配置映射
        SetCollegeConfigRequest request = new SetCollegeConfigRequest();
        request.setEnableSelectCollegesList(Collections.emptyMap());

        // 2. 调用代理实例并捕获异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> selectionPolicyControllerProxy.setCollegeConfig(request));

        // 3. 断言错误文案精确匹配且底层服务未被执行
        assertEquals(CodeBindMessageEnums.PARAMS_ERROR, exception.getCodeBindMessageEnums());
        assertEquals("请至少选择一个学院后再配置", exception.getExceptionMessage());
        verify(selectionPolicyService, never()).setCollegeConfig(any());
    }

    // 场景：测试教师新增课题时课题题目为空被切面拦截
    @Test
    void addTopic_shouldInterceptBlankTopicTitle() {
        // 1. 准备题目标题为空的请求体
        AddTopicRequest request = new AddTopicRequest();
        request.setTopic("");
        request.setType("工程设计");
        request.setDescription("这是一个合法的题目详细描述");
        request.setRequirement("熟悉 Java 开发");
        request.setTopicGroupId(1L);

        // 2. 调用代理实例并捕获异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> topicControllerProxy.addTopic(request));

        // 3. 断言错误文案精确匹配且底层服务未被执行
        assertEquals(CodeBindMessageEnums.PARAMS_ERROR, exception.getCodeBindMessageEnums());
        assertEquals("题目标题不能为空", exception.getExceptionMessage());
        verify(topicApplicationService, never()).addTopic(any());
    }

    // 场景：测试 AI 发送消息时空消息内容被切面拦截
    @Test
    void aiSend_shouldInterceptBlankContent() {
        // 1. 准备消息内容为空的请求体
        AiSendRequest request = new AiSendRequest();
        request.setContent("   ");

        // 2. 调用代理实例并捕获异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> aiControllerProxy.aiSend(request));

        // 3. 断言错误文案精确匹配且底层服务未被执行
        assertEquals(CodeBindMessageEnums.PARAMS_ERROR, exception.getCodeBindMessageEnums());
        assertEquals("消息内容不能为空", exception.getExceptionMessage());
        verify(aiApplicationService, never()).aiSend(any());
    }

    // 场景：测试合法请求体验证通过并成功执行控制器与底层服务
    @Test
    void validRequest_shouldPassValidationAndInvokeService() {
        // 1. 准备合法的添加学院请求体
        CollegeAddRequest request = new CollegeAddRequest();
        request.setCollegeName("计算机学院");
        when(organizationApplicationService.addCollege(request)).thenReturn(100L);

        // 2. 调用代理实例
        BaseResponse<Long> response = organizationControllerProxy.addCollege(request);

        // 3. 断言切面放行且服务被正确调用返回预期响应
        assertNotNull(response);
        assertEquals(0, response.getCode());
        assertEquals(100L, response.getData());
        verify(organizationApplicationService).addCollege(request);
    }

    // 场景：测试删除用户时用户账号为空被切面拦截且底层服务不被调用
    @Test
    void deleteUser_shouldInterceptBlankUserAccount() {
        // 1. 准备用户账号为空白的删除请求体
        DeleteRequest request = new DeleteRequest();
        request.setUserAccount("   ");

        // 2. 调用代理实例并捕获异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> userControllerProxy.deleteUser(request));

        // 3. 断言错误文案精确匹配且底层服务未被执行
        assertEquals(CodeBindMessageEnums.PARAMS_ERROR, exception.getCodeBindMessageEnums());
        assertEquals("指定删除的用户账号不能为空", exception.getExceptionMessage());
        verify(userApplicationService, never()).deleteUser(any());
    }

    // 场景：测试更新用户时用户标识小于 1 被切面拦截且底层服务不被调用
    @Test
    void updateUser_shouldInterceptInvalidId() {
        // 1. 准备用户标识不合法的更新请求体
        UserUpdateRequest request = new UserUpdateRequest();
        request.setId(0L);

        // 2. 调用代理实例并捕获异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> userControllerProxy.updateUser(request));

        // 3. 断言错误文案精确匹配且底层服务未被执行
        assertEquals(CodeBindMessageEnums.PARAMS_ERROR, exception.getCodeBindMessageEnums());
        assertEquals("用户标识不合法, 必须为正整数", exception.getExceptionMessage());
        verify(userApplicationService, never()).updateUser(any());
    }

}
