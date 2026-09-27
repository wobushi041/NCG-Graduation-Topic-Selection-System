package cn.com.edtechhub.worktopicselection.service;

import cn.com.edtechhub.worktopicselection.model.request.selection.SelectStudentRequest;
import cn.com.edtechhub.worktopicselection.model.request.selection.SelectTopicByIdRequest;
import cn.com.edtechhub.worktopicselection.model.request.selection.WithdrawRequest;

/**
 * 学生选题与教师确认写用例服务接口
 *
 * @author wobushi041
 */
public interface TopicSelectionApplicationService {

    /// 选题写用例 ///

    /**
     * 为当前登录学生执行课题预选或取消预选操作
     *
     * @param request 预选或取消预选请求
     * @return 操作关联的题目 id
     */
    Long preselectTopicById(SelectTopicByIdRequest request);

    /**
     * 为当前登录学生提交并确认最终选题
     *
     * @param request 确认提交选题请求
     * @return 选题关联记录 id
     */
    Long selectTopicById(SelectTopicByIdRequest request);

    /**
     * 由当前登录教师直接为指定学生确认提交本人名下课题
     *
     * @param request 教师选择学生请求
     * @return 选题关联记录 id 字符串
     */
    String selectStudent(SelectStudentRequest request);

    /**
     * 由当前登录教师或学生退选已确认的最终课题并恢复课题余量
     *
     * @param request 退选请求
     * @return 是否退选成功
     */
    Boolean withdraw(WithdrawRequest request);

}
