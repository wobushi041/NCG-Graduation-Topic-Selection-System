package cn.com.edtechhub.worktopicselection.controller;

import cn.com.edtechhub.worktopicselection.annotation.SentinelRateLimit;
import cn.com.edtechhub.worktopicselection.exception.CodeBindMessageEnums;
import cn.com.edtechhub.worktopicselection.model.entity.User;
import cn.com.edtechhub.worktopicselection.model.request.user.DeleteRequest;
import cn.com.edtechhub.worktopicselection.model.request.user.TeacherQueryRequest;
import cn.com.edtechhub.worktopicselection.model.request.user.UserAddRequest;
import cn.com.edtechhub.worktopicselection.model.request.user.UserQueryRequest;
import cn.com.edtechhub.worktopicselection.model.request.user.UserUpdateRequest;
import cn.com.edtechhub.worktopicselection.model.vo.LoginUserVO;
import cn.com.edtechhub.worktopicselection.model.vo.TeacherVO;
import cn.com.edtechhub.worktopicselection.model.vo.UserVO;
import cn.com.edtechhub.worktopicselection.response.BaseResponse;
import cn.com.edtechhub.worktopicselection.response.TheResult;
import cn.com.edtechhub.worktopicselection.service.UserApplicationService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户管理与资料控制层
 *
 * @author wobushi041
 */
@RestController
@RequestMapping("/user")
public class UserController {

    /**
     * 注入用户管理应用服务依赖
     */
    private final UserApplicationService userApplicationService;

    /**
     * 构造用户管理与资料控制层实例
     *
     * @param userApplicationService 用户管理应用服务
     */
    public UserController(UserApplicationService userApplicationService) {
        this.userApplicationService = userApplicationService;
    }

    /// 用户相关接口 ///

    /**
     * 创建用户接口
     *
     * @param request 创建用户请求
     * @return 新创建的用户 id
     */
    @SentinelRateLimit(resource = "user.manage.add")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/add")
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest request) {
        return userApplicationService.addUser(request);
    }

    /**
     * 删除用户接口
     *
     * @param request 删除用户请求
     * @return 是否删除成功
     */
    @SentinelRateLimit(resource = "user.manage.delete")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, userApplicationService.deleteUser(request));
    }

    /**
     * 更新用户接口
     *
     * @param request 更新用户请求
     * @return 是否更新成功
     */
    @SentinelRateLimit(resource = "user.manage.update")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/update")
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, userApplicationService.updateUser(request));
    }

    /**
     * 获取当前登录用户数据
     *
     * @return 当前登录用户脱敏视图对象
     */
    @SentinelRateLimit(resource = "user.query.current")
    @SaCheckLogin
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser() {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, userApplicationService.getLoginUser());
    }

    /**
     * 获取用户分页数据
     *
     * @param request 用户分页查询请求
     * @return 用户分页数据
     */
    @SentinelRateLimit(resource = "user.query.page")
    @SaCheckLogin
    @SaCheckRole(value = {"admin", "teacher"}, mode = SaMode.OR)
    @PostMapping("/get/user/page")
    public BaseResponse<Page<User>> listUserByPage(@RequestBody UserQueryRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, userApplicationService.listUserByPage(request));
    }

    /**
     * 获取所有教师的脱敏列表数据接口（教师自己查主任只能获得同系部的主任）
     *
     * @param request 教师查询请求
     * @return 教师脱敏下拉列表数据
     */
    @SentinelRateLimit(resource = "user.query.teacher-list")
    @SaCheckLogin
    @SaCheckRole(value = {"teacher"}, mode = SaMode.OR)
    @PostMapping("/get/teacher")
    public BaseResponse<List<TeacherVO>> getTeacher(@RequestBody TeacherQueryRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, userApplicationService.getTeacher(request));
    }

    /**
     * 根据 id 获取用户数据
     *
     * @param id 用户 id
     * @return 用户实体数据
     */
    @SentinelRateLimit(resource = "user.query.by-id")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @GetMapping("/get")
    public BaseResponse<User> getUserById(long id) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, userApplicationService.getUserById(id));
    }

    /**
     * 根据 id 获取用户包装数据（获取脱敏后的数据）
     *
     * @param id 用户 id
     * @return 用户脱敏视图对象
     */
    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    @SentinelRateLimit(resource = "user.query.vo-by-id")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, userApplicationService.getUserVOById(id));
    }

}
