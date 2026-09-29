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
import cn.edu.nfu.topicselection.model.vo.UnpublishTopicResultVO;
import cn.edu.nfu.topicselection.response.BaseResponse;
import cn.edu.nfu.topicselection.response.TheResult;
import cn.edu.nfu.topicselection.service.TopicApplicationService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 课题维护、审核流转、开放时间发布、教师配额管理与 AI 查重控制层
 *
 * @author wobushi041
 */
@RestController
@RequestMapping("/user")
public class TopicController {

    /**
     * 注入课题全生命周期应用服务依赖
     */
    private final TopicApplicationService topicApplicationService;

    /**
     * 初始化课题控制层
     *
     * @param topicApplicationService 课题全生命周期应用服务
     */
    public TopicController(TopicApplicationService topicApplicationService) {
        this.topicApplicationService = topicApplicationService;
    }

    /// 课题维护与配额接口 ///

    /**
     * 添加选题
     *
     * @param request 添加选题请求
     * @return 新添加的选题 id
     */
    @SentinelRateLimit(resource = "topic.add")
    @SaCheckLogin
    @SaCheckRole(value = {"teacher"}, mode = SaMode.OR)
    @PostMapping("/add/topic")
    public BaseResponse<Long> addTopic(@RequestBody AddTopicRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, topicApplicationService.addTopic(request));
    }

    /**
     * 删除选题
     *
     * @param request 删除选题请求
     * @return 是否删除成功
     */
    @SentinelRateLimit(resource = "topic.delete")
    @SaCheckLogin
    @SaCheckRole(value = {"teacher"}, mode = SaMode.OR)
    @PostMapping("/delete/topic")
    public BaseResponse<Boolean> deleteTopic(@RequestBody DeleteTopicRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, topicApplicationService.deleteTopic(request));
    }

    /**
     * 获取教师题目上限
     *
     * @param request 查询教师题目上限请求
     * @return 教师剩余出题上限
     */
    @SentinelRateLimit(resource = "topic.quota.get")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/get/teacher/topicAmount")
    public BaseResponse<Integer> getTeacherTopicAmount(@RequestBody GetTeacherTopicAmountRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, topicApplicationService.getTeacherTopicAmount(request));
    }

    /**
     * 修改教师题目上限
     *
     * @param request 设置教师题目上限请求
     * @return 是否设置成功
     */
    @SentinelRateLimit(resource = "topic.quota.set")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/set/teacher/topicAmount")
    public BaseResponse<Boolean> setTeacherTopicAmount(@RequestBody SetTeacherTopicAmountRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, topicApplicationService.setTeacherTopicAmount(request));
    }

    /// 课题审核、发布与 AI 查重接口 ///

    /**
     * 审核题目或重新审核题目
     *
     * @param request 审核题目请求
     * @return 是否审核处理成功
     */
    @SentinelRateLimit(resource = "topic.review.check")
    @SaCheckLogin
    @SaCheckRole(value = {"topic_leader", "teacher"}, mode = SaMode.OR)
    @PostMapping("/check/topic")
    public BaseResponse<Boolean> checkTopic(@RequestBody CheckTopicRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, topicApplicationService.checkTopic(request));
    }

    /**
     * 根据题目 id 列表添加开放的开始时间和结束时间来发布选题列表
     *
     * @param request 设置选题开放时间请求
     * @return 成功取消与因业务限制跳过的课题处理结果
     */
    @SentinelRateLimit(resource = "topic.publication.publish")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/set/time/by/id")
    public BaseResponse<String> setTimeById(@RequestBody SetTimeRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, topicApplicationService.setTimeById(request));
    }

    /**
     * 根据题目 id 列表取消已发布的选题列表并置为空时间
     *
     * @param request 取消选题开放时间请求
     * @return 操作结果提示信息
     */
    @SentinelRateLimit(resource = "topic.publication.unpublish")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/unset/time/by/id")
    public BaseResponse<UnpublishTopicResultVO> unsetTimeById(@RequestBody UnSetTimeRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, topicApplicationService.unsetTimeById(request));
    }

    /**
     * 更新选题信息
     *
     * @param request 更新选题请求
     * @return 更新结果提示信息
     */
    @SentinelRateLimit(resource = "topic.update")
    @SaCheckLogin
    @SaCheckRole(value = {"teacher"}, mode = SaMode.OR)
    @PostMapping("/update/topic")
    public BaseResponse<String> updateTopic(@RequestBody UpdateTopicRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, topicApplicationService.updateTopic(request));
    }

    /**
     * 获取题目审核等级
     *
     * @param request 题目 AI 查重检测请求
     * @return AI 查重检测结果
     */
    @SentinelRateLimit(resource = "topic.review.ai-level")
    @SaCheckLogin
    @SaCheckRole(value = {"admin", "teacher"}, mode = SaMode.OR)
    @PostMapping("/get/topic/review_level")
    public BaseResponse<AIResult> getTopicReviewLevel(@RequestBody GetTopicReviewLevelRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, topicApplicationService.getTopicReviewLevel(request));
    }

}
