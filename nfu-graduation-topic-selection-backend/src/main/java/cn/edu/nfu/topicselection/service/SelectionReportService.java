package cn.edu.nfu.topicselection.service;

import cn.edu.nfu.topicselection.model.entity.Topic;
import cn.edu.nfu.topicselection.model.entity.User;
import cn.edu.nfu.topicselection.model.request.topic.TopicQueryByAdminRequest;
import cn.edu.nfu.topicselection.model.request.topic.TopicQueryRequest;
import cn.edu.nfu.topicselection.model.request.user.TopicLeaderQueryRequest;
import cn.edu.nfu.topicselection.model.request.user.GetUserListRequest;
import cn.edu.nfu.topicselection.model.request.user.UserQueryRequest;
import cn.edu.nfu.topicselection.model.vo.TopicLeaderVO;
import cn.edu.nfu.topicselection.model.vo.SituationVO;
import cn.edu.nfu.topicselection.model.vo.UserNameVO;
import cn.edu.nfu.topicselection.model.vo.UserVO;
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
     * 分页查询学院教师选题余量与已选数量统计列表
     *
     * @param request 学院教师分页查询请求
     * @return 学院教师统计分页数据
     */
    Page<TopicLeaderVO> getTeacher(TopicLeaderQueryRequest request);

    /**
     * 查询当前登录选题负责人所属学院中尚未选题的学生列表
     *
     * @return 同学院未选题学生列表
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
     * 选题负责人分页查询本学院存在待审核题目的教师统计列表
     *
     * @param request 学院教师查询请求
     * @return 待审核题目的学院教师分页数据
     */
    Page<TopicLeaderVO> getTeacherByAdmin(TopicLeaderQueryRequest request);

}
