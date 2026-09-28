package cn.edu.nfu.topicselection.controller;

import cn.edu.nfu.topicselection.annotation.SentinelRateLimit;
import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import cn.edu.nfu.topicselection.manager.ai.AIResult;
import cn.edu.nfu.topicselection.model.request.topic.AddTopicRequest;
import cn.edu.nfu.topicselection.model.request.topic.CheckTopicRequest;
import cn.edu.nfu.topicselection.model.request.topic.DeleteTopicRequest;
import cn.edu.nfu.topicselection.model.request.topic.GetTeacherTopicAmountRequest;
import cn.edu.nfu.topicselection.model.request.topic.GetTopicReviewLevelRequest;
import cn.edu.nfu.topicselection.model.request.topic.SetTeacherTopicAmountRequest;
import cn.edu.nfu.topicselection.model.request.topic.SetTimeRequest;
import cn.edu.nfu.topicselection.model.request.topic.UnSetTimeRequest;
import cn.edu.nfu.topicselection.model.request.topic.UpdateTopicRequest;
import cn.edu.nfu.topicselection.response.BaseResponse;
import cn.edu.nfu.topicselection.service.TopicApplicationService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 课题控制器路由契约、鉴权注解与 Sentinel 限流绑定测试
 *
 * @author wobushi041
 */
@ExtendWith(MockitoExtension.class)
class TopicControllerContractTest {

    /**
     * 待测课题控制器
     */
    private TopicController topicController;

    /**
     * 模拟课题全生命周期应用服务
     */
    @Mock
    private TopicApplicationService topicApplicationService;

    @BeforeEach
    void setUp() {
        topicController = new TopicController(topicApplicationService);
    }

    // 场景：测试课题控制器保持 /user 根路径映射与全部 9 个端点的路由、鉴权和限流注解契约
    @Test
    void topicControllerMaintainsRouteAuthAndRateLimitContracts() throws Exception {
        // 1. 校验控制器类级 @RequestMapping("/user") 注解
        RequestMapping classMapping = TopicController.class.getAnnotation(RequestMapping.class);
        assertNotNull(classMapping);
        assertArrayEquals(new String[]{"/user"}, classMapping.value());

        // 2. 逐一校验 9 个课题接口的 PostMapping 路由、@SaCheckLogin、@SaCheckRole 与 @SentinelRateLimit 资源名
        assertEndpointContract("addTopic", AddTopicRequest.class, "/add/topic", "topic.add", new String[]{"teacher"});
        assertEndpointContract("deleteTopic", DeleteTopicRequest.class, "/delete/topic", "topic.delete", new String[]{"teacher"});
        assertEndpointContract("getTeacherTopicAmount", GetTeacherTopicAmountRequest.class, "/get/teacher/topicAmount", "topic.quota.get", new String[]{"admin"});
        assertEndpointContract("setTeacherTopicAmount", SetTeacherTopicAmountRequest.class, "/set/teacher/topicAmount", "topic.quota.set", new String[]{"admin"});
        assertEndpointContract("checkTopic", CheckTopicRequest.class, "/check/topic", "topic.review.check", new String[]{"dept", "teacher"});
        assertEndpointContract("setTimeById", SetTimeRequest.class, "/set/time/by/id", "topic.publication.publish", new String[]{"admin"});
        assertEndpointContract("unsetTimeById", UnSetTimeRequest.class, "/unset/time/by/id", "topic.publication.unpublish", new String[]{"admin"});
        assertEndpointContract("updateTopic", UpdateTopicRequest.class, "/update/topic", "topic.update", new String[]{"teacher"});
        assertEndpointContract("getTopicReviewLevel", GetTopicReviewLevelRequest.class, "/get/topic/review_level", "topic.review.ai-level", new String[]{"admin", "teacher"});

        // 3. 断言 UserController 中已完全移除上述 9 个课题接口方法
        assertTrue(Arrays.stream(UserController.class.getDeclaredMethods())
                .noneMatch(method -> Arrays.asList(
                        "addTopic",
                        "deleteTopic",
                        "getTeacherTopicAmount",
                        "setTeacherTopicAmount",
                        "checkTopic",
                        "setTimeById",
                        "unsetTimeById",
                        "updateTopic",
                        "getTopicReviewLevel"
                ).contains(method.getName())));
    }

    // 场景：测试课题控制器将全部 9 个端点请求委托给课题应用服务并封装标准响应
    @Test
    void topicControllerDelegatesAllEndpointsToApplicationService() {
        // 1. 准备 9 个课题接口的请求对象与模拟返回值
        AddTopicRequest addRequest = new AddTopicRequest();
        DeleteTopicRequest deleteRequest = new DeleteTopicRequest();
        GetTeacherTopicAmountRequest getQuotaRequest = new GetTeacherTopicAmountRequest();
        SetTeacherTopicAmountRequest setQuotaRequest = new SetTeacherTopicAmountRequest();
        CheckTopicRequest checkRequest = new CheckTopicRequest();
        SetTimeRequest setTimeRequest = new SetTimeRequest();
        UnSetTimeRequest unsetTimeRequest = new UnSetTimeRequest();
        UpdateTopicRequest updateRequest = new UpdateTopicRequest();
        GetTopicReviewLevelRequest aiReviewRequest = new GetTopicReviewLevelRequest();
        AIResult aiResult = new AIResult();

        when(topicApplicationService.addTopic(addRequest)).thenReturn(101L);
        when(topicApplicationService.deleteTopic(deleteRequest)).thenReturn(true);
        when(topicApplicationService.getTeacherTopicAmount(getQuotaRequest)).thenReturn(8);
        when(topicApplicationService.setTeacherTopicAmount(setQuotaRequest)).thenReturn(true);
        when(topicApplicationService.checkTopic(checkRequest)).thenReturn(true);
        when(topicApplicationService.setTimeById(setTimeRequest)).thenReturn("成功开放题目!");
        when(topicApplicationService.unsetTimeById(unsetTimeRequest)).thenReturn("成功取消发布!");
        when(topicApplicationService.updateTopic(updateRequest)).thenReturn("更新成功");
        when(topicApplicationService.getTopicReviewLevel(aiReviewRequest)).thenReturn(aiResult);

        // 2. 依次调用课题控制器的 9 个接口方法
        BaseResponse<Long> addResponse = topicController.addTopic(addRequest);
        BaseResponse<Boolean> deleteResponse = topicController.deleteTopic(deleteRequest);
        BaseResponse<Integer> getQuotaResponse = topicController.getTeacherTopicAmount(getQuotaRequest);
        BaseResponse<Boolean> setQuotaResponse = topicController.setTeacherTopicAmount(setQuotaRequest);
        BaseResponse<Boolean> checkResponse = topicController.checkTopic(checkRequest);
        BaseResponse<String> setTimeResponse = topicController.setTimeById(setTimeRequest);
        BaseResponse<String> unsetTimeResponse = topicController.unsetTimeById(unsetTimeRequest);
        BaseResponse<String> updateResponse = topicController.updateTopic(updateRequest);
        BaseResponse<AIResult> aiReviewResponse = topicController.getTopicReviewLevel(aiReviewRequest);

        // 3. 断言响应码与返回数据完全匹配且应用服务被准确调用
        assertEquals(CodeBindMessageEnums.SUCCESS.getCode(), addResponse.getCode());
        assertEquals(101L, addResponse.getData());
        assertTrue(deleteResponse.getData());
        assertEquals(8, getQuotaResponse.getData());
        assertTrue(setQuotaResponse.getData());
        assertTrue(checkResponse.getData());
        assertEquals("成功开放题目!", setTimeResponse.getData());
        assertEquals("成功取消发布!", unsetTimeResponse.getData());
        assertEquals("更新成功", updateResponse.getData());
        assertEquals(aiResult, aiReviewResponse.getData());

        verify(topicApplicationService).addTopic(addRequest);
        verify(topicApplicationService).deleteTopic(deleteRequest);
        verify(topicApplicationService).getTeacherTopicAmount(getQuotaRequest);
        verify(topicApplicationService).setTeacherTopicAmount(setQuotaRequest);
        verify(topicApplicationService).checkTopic(checkRequest);
        verify(topicApplicationService).setTimeById(setTimeRequest);
        verify(topicApplicationService).unsetTimeById(unsetTimeRequest);
        verify(topicApplicationService).updateTopic(updateRequest);
        verify(topicApplicationService).getTopicReviewLevel(aiReviewRequest);
    }

    /**
     * 校验指定控制器方法的路由、登录鉴权、角色鉴权与 Sentinel 限流注解
     *
     * @param methodName      方法名
     * @param parameterType   请求参数类型
     * @param expectedPath    预期 POST 路由路径
     * @param expectedLimit   预期 Sentinel 资源名
     * @param expectedRoles   预期允许的角色数组
     */
    private static void assertEndpointContract(
            String methodName,
            Class<?> parameterType,
            String expectedPath,
            String expectedLimit,
            String[] expectedRoles
    ) throws Exception {
        Method method = TopicController.class.getDeclaredMethod(methodName, parameterType);
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        SaCheckLogin checkLogin = method.getAnnotation(SaCheckLogin.class);
        SaCheckRole checkRole = method.getAnnotation(SaCheckRole.class);
        SentinelRateLimit rateLimit = method.getAnnotation(SentinelRateLimit.class);

        assertNotNull(postMapping, methodName + " 缺少 @PostMapping");
        assertArrayEquals(new String[]{expectedPath}, postMapping.value());
        assertNotNull(checkLogin, methodName + " 缺少显式 @SaCheckLogin");
        assertNotNull(checkRole, methodName + " 缺少 @SaCheckRole");
        assertArrayEquals(expectedRoles, checkRole.value());
        assertNotNull(rateLimit, methodName + " 缺少 @SentinelRateLimit");
        assertEquals(expectedLimit, rateLimit.resource());
    }

}
