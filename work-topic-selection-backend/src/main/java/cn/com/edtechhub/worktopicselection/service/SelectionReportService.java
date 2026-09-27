package cn.com.edtechhub.worktopicselection.service;

import cn.com.edtechhub.worktopicselection.model.entity.Topic;
import cn.com.edtechhub.worktopicselection.model.entity.User;
import cn.com.edtechhub.worktopicselection.model.request.topic.TopicQueryByAdminRequest;
import cn.com.edtechhub.worktopicselection.model.request.topic.TopicQueryRequest;
import cn.com.edtechhub.worktopicselection.model.request.user.DeptTeacherQueryRequest;
import cn.com.edtechhub.worktopicselection.model.request.user.GetUserListRequest;
import cn.com.edtechhub.worktopicselection.model.request.user.UserQueryRequest;
import cn.com.edtechhub.worktopicselection.model.vo.DeptTeacherVO;
import cn.com.edtechhub.worktopicselection.model.vo.SituationVO;
import cn.com.edtechhub.worktopicselection.model.vo.UserNameVO;
import cn.com.edtechhub.worktopicselection.model.vo.UserVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 选题题目与统计报表只读查询服务接口
 *
 * @author wobushi041
 */
public interface SelectionReportService {

    /// 选题与用户统计只读查询服务契约 ///

    /**
     * 按当前登录用户角色权限分页查询可见的选题列表
     *
     * @param request 选题分页查询请求
     * @return 选题分页数据
     */
    Page<Topic> getTopicList(TopicQueryRequest request);

    /**
     * 查询当前登录用户权限范围内的学生选题统计汇总情况
     *
     * @return 选题统计情况视图对象
     */
    SituationVO getSelectTopicSituation();

    /**
     * 分页查询系部教师选题余量与已选数量统计列表
     *
     * @param request 系部教师分页查询请求
     * @return 系部教师统计分页数据
     */
    Page<DeptTeacherVO> getTeacher(DeptTeacherQueryRequest request);

    /**
     * 查询当前登录系主任所属系部中尚未选题的学生列表
     *
     * @return 同系部未选题学生列表
     */
    List<User> getUnSelectTopicStudentList();

    /**
     * 管理员分页查询全量选题列表
     *
     * @param request 管理员查询题目分页请求
     * @return 选题分页数据
     */
    Page<Topic> getTopicListByAdmin(TopicQueryByAdminRequest request);

    /**
     * 管理员分页查询脱敏后的用户视图列表
     *
     * @param request 用户分页查询请求
     * @return 用户脱敏视图分页数据
     */
    Page<UserVO> listUserVOByPage(UserQueryRequest request);

    /**
     * 管理员按指定角色查询用户姓名视图列表
     *
     * @param request 获取用户姓名列表请求
     * @return 用户姓名视图列表
     */
    List<UserNameVO> getUserList(GetUserListRequest request);

    /**
     * 系主任分页查询本系部存在待审核题目的教师统计列表
     *
     * @param request 系部教师查询请求
     * @return 待审核题目的系部教师分页数据
     */
    Page<DeptTeacherVO> getTeacherByAdmin(DeptTeacherQueryRequest request);

}
