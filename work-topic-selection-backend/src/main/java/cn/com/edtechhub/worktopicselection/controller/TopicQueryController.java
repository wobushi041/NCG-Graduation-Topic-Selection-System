package cn.com.edtechhub.worktopicselection.controller;

import cn.com.edtechhub.worktopicselection.annotation.SentinelRateLimit;
import cn.com.edtechhub.worktopicselection.exception.CodeBindMessageEnums;
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
import cn.com.edtechhub.worktopicselection.response.BaseResponse;
import cn.com.edtechhub.worktopicselection.response.TheResult;
import cn.com.edtechhub.worktopicselection.service.SelectionReportService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 选题题目与统计报表只读查询控制层
 *
 * @author wobushi041
 */
@RestController
@RequestMapping("/user")
public class TopicQueryController {

    /**
     * 注入选题题目与统计报表只读查询服务依赖
     */
    private final SelectionReportService selectionReportService;

    /**
     * 构造选题题目与统计报表只读查询控制层实例
     *
     * @param selectionReportService 选题题目与统计报表只读查询服务
     */
    public TopicQueryController(SelectionReportService selectionReportService) {
        this.selectionReportService = selectionReportService;
    }

    /// 选题题目与统计只读查询接口 ///

    /**
     * 获取选题分页数据
     *
     * @param request 选题分页查询请求
     * @return 选题分页数据
     */
    @SentinelRateLimit(resource = "query.topic.page")
    @SaCheckLogin
    // @CacheSearchOptimization(ttl = 15, modelClass = Topic.class)
    @PostMapping("/get/topic/page")
    public BaseResponse<Page<Topic>> getTopicList(@RequestBody TopicQueryRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, selectionReportService.getTopicList(request));
    }

    /**
     * 获取当前的选题情况（只能获取和当前登陆用户系部相同的选题）
     *
     * @return 选题统计情况视图对象
     */
    @SentinelRateLimit(resource = "query.selection.situation")
    @SaCheckLogin
    @SaCheckRole(value = {"admin", "dept"}, mode = SaMode.OR)
    @PostMapping("/get/select/topic/situation")
    public BaseResponse<SituationVO> getSelectTopicSituation() {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, selectionReportService.getSelectTopicSituation());
    }

    /**
     * 获取系部教师数据
     *
     * @param request 系部教师分页查询请求
     * @return 系部教师统计分页数据
     */
    @SentinelRateLimit(resource = "query.dept.teacher")
    @SaCheckLogin
    // @CacheSearchOptimization(ttl = 30, modelClass = DeptTeacherVO.class)
    @PostMapping("/get/dept/teacher")
    public BaseResponse<Page<DeptTeacherVO>> getTeacher(@RequestBody DeptTeacherQueryRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, selectionReportService.getTeacher(request));
    }

    /**
     * 获取和当前登陆用户同系的没有选题的学生
     *
     * @return 同系部未选题学生列表
     */
    @SentinelRateLimit(resource = "query.selection.unselected-students")
    @SaCheckLogin
    @SaCheckRole(value = {"dept"}, mode = SaMode.OR)
    @PostMapping("/get/unselect/topic/student/list")
    public BaseResponse<List<User>> getUnSelectTopicStudentList() {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, selectionReportService.getUnSelectTopicStudentList());
    }

    /**
     * 管理员获取题目
     *
     * @param request 管理员查询题目分页请求
     * @return 选题分页数据
     */
    @SentinelRateLimit(resource = "query.topic.admin-page")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/get/topic/list/by/admin")
    public BaseResponse<Page<Topic>> getTopicListByAdmin(@RequestBody TopicQueryByAdminRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, selectionReportService.getTopicListByAdmin(request));
    }

    /**
     * 分页获取用户封装列表
     *
     * @param request 用户分页查询请求
     * @return 用户脱敏视图分页数据
     */
    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    @SentinelRateLimit(resource = "query.user.vo-page")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, selectionReportService.listUserVOByPage(request));
    }

    /**
     * 获取用户列表数据
     *
     * @param request 获取用户姓名列表请求
     * @return 用户姓名视图列表
     */
    @SentinelRateLimit(resource = "query.user.name-list")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/get/user/list")
    public BaseResponse<List<UserNameVO>> getUserList(@RequestBody GetUserListRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, selectionReportService.getUserList(request));
    }

    /**
     * 获取待审核题目的系部教师列表
     *
     * @param request 系部教师查询请求
     * @return 待审核题目的系部教师分页数据
     */
    @SentinelRateLimit(resource = "query.dept.pending-teacher")
    @SaCheckLogin
    @SaCheckRole(value = {"dept"}, mode = SaMode.OR)
    @PostMapping("/get/dept/teacher/by/admin")
    public BaseResponse<Page<DeptTeacherVO>> getTeacherByAdmin(@RequestBody DeptTeacherQueryRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, selectionReportService.getTeacherByAdmin(request));
    }

}
