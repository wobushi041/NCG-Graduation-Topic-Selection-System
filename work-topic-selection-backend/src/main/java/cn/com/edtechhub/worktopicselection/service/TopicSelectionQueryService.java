package cn.com.edtechhub.worktopicselection.service;

import cn.com.edtechhub.worktopicselection.model.entity.Topic;
import cn.com.edtechhub.worktopicselection.model.entity.User;
import cn.com.edtechhub.worktopicselection.model.request.selection.GetSelectTopicByIdRequest;
import cn.com.edtechhub.worktopicselection.model.request.selection.GetSelectTopicRequest;
import cn.com.edtechhub.worktopicselection.model.request.selection.GetStudentByTopicIdRequest;

import java.util.List;

/**
 * 学生选题与教师确认读用例服务接口
 *
 * @author wobushi041
 */
public interface TopicSelectionQueryService {

    /// 选题读用例 ///

    /**
     * 查询已确认选择当前登录教师名下指定题目的学生列表
     *
     * @param request 根据题目 id 查询已选学生请求
     * @return 已选该题目的学生列表
     */
    List<User> getSelectTopicById(GetSelectTopicByIdRequest request);

    /**
     * 查询当前登录学生已预选的题目列表
     *
     * @return 当前学生预选的题目列表
     */
    List<Topic> getPreTopic();

    /**
     * 查询当前登录学生已确认的最终选题列表
     *
     * @return 当前学生最终确认的题目列表
     */
    List<Topic> getSelectTopic();

    /**
     * 查询当前登录学生最终确认指定题目的时间戳字符串
     *
     * @param request 查询最终选题时间请求
     * @return 最终选题确认时间戳（秒）字符串
     */
    String getSelectTopicTime(GetSelectTopicRequest request);

    /**
     * 按题目 id 查询已确认选择当前登录教师名下该题目的学生列表
     *
     * @param request 根据题目 id 查询学生请求
     * @return 已选择该题目的学生列表
     */
    List<User> getStudentByTopicId(GetStudentByTopicIdRequest request);

}
