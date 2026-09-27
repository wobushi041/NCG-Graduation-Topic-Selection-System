package cn.com.edtechhub.worktopicselection.service;

import cn.com.edtechhub.worktopicselection.manager.ai.AIResult;
import cn.com.edtechhub.worktopicselection.model.request.topic.AddTopicRequest;
import cn.com.edtechhub.worktopicselection.model.request.topic.CheckTopicRequest;
import cn.com.edtechhub.worktopicselection.model.request.topic.DeleteTopicRequest;
import cn.com.edtechhub.worktopicselection.model.request.topic.GetTeacherTopicAmountRequest;
import cn.com.edtechhub.worktopicselection.model.request.topic.GetTopicReviewLevelRequest;
import cn.com.edtechhub.worktopicselection.model.request.topic.SetTeacherTopicAmountRequest;
import cn.com.edtechhub.worktopicselection.model.request.topic.SetTimeRequest;
import cn.com.edtechhub.worktopicselection.model.request.topic.UnSetTimeRequest;
import cn.com.edtechhub.worktopicselection.model.request.topic.UpdateTopicRequest;

/**
 * 定义课题维护、审核流转、开放时间发布、教师配额管理与 AI 查重检测的应用服务契约
 *
 * @author wobushi041
 */
public interface TopicApplicationService {

    /**
     * 添加课题并扣减当前教师剩余出题配额
     *
     * @param request 添加题目请求
     * @return 新添加的选题 id
     */
    Long addTopic(AddTopicRequest request);

    /**
     * 删除本人发布的课题、清理关联选题记录并恢复教师出题配额
     *
     * @param request 删除题目请求
     * @return 是否删除成功
     */
    Boolean deleteTopic(DeleteTopicRequest request);

    /**
     * 查询指定教师的剩余可出题上限数量
     *
     * @param request 获取教师题目上限请求
     * @return 教师剩余出题上限数量
     */
    Integer getTeacherTopicAmount(GetTeacherTopicAmountRequest request);

    /**
     * 修改指定教师的可出题上限数量
     *
     * @param request 设置教师题目上限请求
     * @return 是否设置成功
     */
    Boolean setTeacherTopicAmount(SetTeacherTopicAmountRequest request);

    /**
     * 审核课题或将退回课题重新提交审核
     *
     * @param request 题目审核请求
     * @return 是否审核处理成功
     */
    Boolean checkTopic(CheckTopicRequest request);

    /**
     * 按课题 id 列表批量设置开放时间窗口并发布课题
     *
     * @param request 设置选题开放时间请求
     * @return 操作结果提示信息
     */
    String setTimeById(SetTimeRequest request);

    /**
     * 按课题 id 列表批量取消已发布课题并清空开放时间窗口
     *
     * @param request 取消设置选题开放时间请求
     * @return 操作结果提示信息
     */
    String unsetTimeById(UnSetTimeRequest request);

    /**
     * 更新本人课题信息并重置为待审核状态
     *
     * @param request 修改题目请求
     * @return 更新结果提示信息
     */
    String updateTopic(UpdateTopicRequest request);

    /**
     * 执行课题 AI 查重审核等级检测
     *
     * @param request 获取题目审核等级请求
     * @return AI 查重检测结果
     */
    AIResult getTopicReviewLevel(GetTopicReviewLevelRequest request);

}
