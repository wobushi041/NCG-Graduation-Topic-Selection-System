package cn.com.edtechhub.worktopicselection.controller;

import cn.com.edtechhub.worktopicselection.annotation.SentinelRateLimit;
import cn.com.edtechhub.worktopicselection.annotation.ValidateRequest;
import cn.com.edtechhub.worktopicselection.exception.CodeBindMessageEnums;
import cn.com.edtechhub.worktopicselection.model.entity.Topic;
import cn.com.edtechhub.worktopicselection.model.entity.User;
import cn.com.edtechhub.worktopicselection.model.request.selection.GetSelectTopicByIdRequest;
import cn.com.edtechhub.worktopicselection.model.request.selection.GetSelectTopicRequest;
import cn.com.edtechhub.worktopicselection.model.request.selection.GetStudentByTopicIdRequest;
import cn.com.edtechhub.worktopicselection.model.request.selection.SelectStudentRequest;
import cn.com.edtechhub.worktopicselection.model.request.selection.SelectTopicByIdRequest;
import cn.com.edtechhub.worktopicselection.model.request.selection.WithdrawRequest;
import cn.com.edtechhub.worktopicselection.response.BaseResponse;
import cn.com.edtechhub.worktopicselection.response.TheResult;
import cn.com.edtechhub.worktopicselection.service.TopicSelectionApplicationService;
import cn.com.edtechhub.worktopicselection.service.TopicSelectionQueryService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学生选题与教师确认 HTTP 入口
 *
 * @author wobushi041
 */
@RestController
@RequestMapping("/user")
public class TopicSelectionController {

    /**
     * 注入学生选题与教师确认写用例服务依赖
     */
    private final TopicSelectionApplicationService topicSelectionApplicationService;

    /**
     * 注入学生选题与教师确认读用例服务依赖
     */
    private final TopicSelectionQueryService topicSelectionQueryService;

    /**
     * 初始化学生选题与教师确认 HTTP 入口
     *
     * @param topicSelectionApplicationService 学生选题与教师确认写用例服务
     * @param topicSelectionQueryService       学生选题与教师确认读用例服务
     */
    public TopicSelectionController(TopicSelectionApplicationService topicSelectionApplicationService,
                                    TopicSelectionQueryService topicSelectionQueryService) {
        this.topicSelectionApplicationService = topicSelectionApplicationService;
        this.topicSelectionQueryService = topicSelectionQueryService;
    }

    /// 选题写用例 ///

    /**
     * 根据题目 id 进行预先选题的操作（确认预先选题和取消预先选题）
     *
     * @param request 预选或取消预选请求
     * @return 操作关联的题目 id
     */
    @SaCheckLogin
    @SaCheckRole(value = {"student"}, mode = SaMode.OR)
    @ValidateRequest
    @SentinelRateLimit(resource = "topic.selection.preselect")
    @PostMapping("/preselect/topic/by/id")
    public BaseResponse<Long> preSelectTopicById(@RequestBody SelectTopicByIdRequest request) {
        return TheResult.success(
                CodeBindMessageEnums.SUCCESS,
                topicSelectionApplicationService.preselectTopicById(request)
        );
    }

    /**
     * 根据题目 id 进行提交选题的操作（确认提交选题）
     *
     * @param request 确认提交选题请求
     * @return 选题关联记录 id
     */
    @SaCheckLogin
    @SaCheckRole(value = {"student"}, mode = SaMode.OR)
    @ValidateRequest
    @SentinelRateLimit(resource = "topic.selection.confirm")
    @PostMapping("/select/topic/by/id")
    public BaseResponse<Long> selectTopicById(@RequestBody SelectTopicByIdRequest request) {
        return TheResult.success(
                CodeBindMessageEnums.SUCCESS,
                topicSelectionApplicationService.selectTopicById(request)
        );
    }

    /**
     * 教师直接帮助学生确认提交题目
     *
     * @param request 教师选择学生请求
     * @return 选题关联记录 id 字符串
     */
    @SaCheckLogin
    @SaCheckRole(value = {"teacher"}, mode = SaMode.OR)
    @ValidateRequest
    @SentinelRateLimit(resource = "topic.selection.assign-student")
    @PostMapping("/select/student")
    public BaseResponse<String> selectStudent(@RequestBody SelectStudentRequest request) {
        return TheResult.success(
                CodeBindMessageEnums.SUCCESS,
                topicSelectionApplicationService.selectStudent(request)
        );
    }

    /**
     * 教师或学生直接取消提交题目
     *
     * @param request 退选请求
     * @return 是否退选成功
     */
    @SaCheckLogin
    @SaCheckRole(value = {"teacher", "student"}, mode = SaMode.OR)
    @ValidateRequest
    @SentinelRateLimit(resource = "topic.selection.withdraw")
    @PostMapping("/withdraw")
    public BaseResponse<Boolean> withdraw(@RequestBody WithdrawRequest request) {
        return TheResult.success(
                CodeBindMessageEnums.SUCCESS,
                topicSelectionApplicationService.withdraw(request)
        );
    }

    /// 选题读用例 ///

    /**
     * 获取选择了自己题目的学生
     *
     * @param request 根据题目 id 查询已选学生请求
     * @return 已选该题目的学生列表
     */
    @SaCheckLogin
    @SaCheckRole(value = {"teacher"}, mode = SaMode.OR)
    @ValidateRequest
    @SentinelRateLimit(resource = "topic.selection.query-selected-students")
    @PostMapping("/get/select/topic/by/id")
    public BaseResponse<List<User>> getSelectTopicById(@RequestBody GetSelectTopicByIdRequest request) {
        return TheResult.success(
                CodeBindMessageEnums.SUCCESS,
                topicSelectionQueryService.getSelectTopicById(request)
        );
    }

    /**
     * 获取当前登陆账号学生的预先选题
     *
     * @return 当前学生预选的题目列表
     */
    @SaCheckLogin
    @SaCheckRole(value = {"student"}, mode = SaMode.OR)
    @SentinelRateLimit(resource = "topic.selection.query-preselected")
    @PostMapping("/get/preselect/topic")
    public BaseResponse<List<Topic>> getPreTopic() {
        return TheResult.success(
                CodeBindMessageEnums.SUCCESS,
                topicSelectionQueryService.getPreTopic()
        );
    }

    /**
     * 获取当前登陆账号学生的最终选题
     *
     * @return 当前学生最终确认的题目列表
     */
    @SaCheckLogin
    @SaCheckRole(value = {"student"}, mode = SaMode.OR)
    @SentinelRateLimit(resource = "topic.selection.query-selected")
    @PostMapping("/get/select/topic")
    public BaseResponse<List<Topic>> getSelectTopic() {
        return TheResult.success(
                CodeBindMessageEnums.SUCCESS,
                topicSelectionQueryService.getSelectTopic()
        );
    }

    /**
     * 获取最终选题记录的选中时间
     *
     * @param request 查询最终选题时间请求
     * @return 最终选题确认时间戳（秒）字符串
     */
    @SaCheckLogin
    @SaCheckRole(value = {"student"}, mode = SaMode.OR)
    @ValidateRequest
    @SentinelRateLimit(resource = "topic.selection.query-choice-time")
    @PostMapping("/get/select/topic/choice_time")
    public BaseResponse<String> getSelectTopicTime(@RequestBody GetSelectTopicRequest request) {
        return TheResult.success(
                CodeBindMessageEnums.SUCCESS,
                topicSelectionQueryService.getSelectTopicTime(request)
        );
    }

    /**
     * 根据题目 id 获取学生
     *
     * @param request 根据题目 id 查询学生请求
     * @return 已选择该题目的学生列表
     */
    @SaCheckLogin
    @SaCheckRole(value = {"teacher"}, mode = SaMode.OR)
    @ValidateRequest
    @SentinelRateLimit(resource = "topic.selection.query-students-by-topic")
    @PostMapping("/get/student/by/topicId")
    public BaseResponse<List<User>> getStudentByTopicId(@RequestBody GetStudentByTopicIdRequest request) {
        return TheResult.success(
                CodeBindMessageEnums.SUCCESS,
                topicSelectionQueryService.getStudentByTopicId(request)
        );
    }

}
