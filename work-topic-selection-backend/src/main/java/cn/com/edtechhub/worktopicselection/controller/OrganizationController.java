package cn.com.edtechhub.worktopicselection.controller;

import cn.com.edtechhub.worktopicselection.annotation.SentinelRateLimit;
import cn.com.edtechhub.worktopicselection.exception.CodeBindMessageEnums;
import cn.com.edtechhub.worktopicselection.model.entity.Dept;
import cn.com.edtechhub.worktopicselection.model.entity.Project;
import cn.com.edtechhub.worktopicselection.model.request.organization.DeleteDeptRequest;
import cn.com.edtechhub.worktopicselection.model.request.organization.DeleteProjectRequest;
import cn.com.edtechhub.worktopicselection.model.request.organization.DeptAddRequest;
import cn.com.edtechhub.worktopicselection.model.request.organization.DeptQueryRequest;
import cn.com.edtechhub.worktopicselection.model.request.organization.ProjectAddRequest;
import cn.com.edtechhub.worktopicselection.model.request.organization.ProjectGroupUpdateRequest;
import cn.com.edtechhub.worktopicselection.model.request.organization.ProjectQueryRequest;
import cn.com.edtechhub.worktopicselection.model.vo.DeptVO;
import cn.com.edtechhub.worktopicselection.model.vo.ProjectVO;
import cn.com.edtechhub.worktopicselection.response.BaseResponse;
import cn.com.edtechhub.worktopicselection.response.TheResult;
import cn.com.edtechhub.worktopicselection.service.OrganizationApplicationService;
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
 * 系部、专业与专业选题组配置控制层
 *
 * @author wobushi041
 */
@RestController
@RequestMapping("/user")
public class OrganizationController {

    /**
     * 注入组织与教师选题组应用服务依赖
     */
    private final OrganizationApplicationService organizationApplicationService;

    /**
     * 初始化组织控制层
     *
     * @param organizationApplicationService 组织与教师选题组应用服务
     */
    public OrganizationController(OrganizationApplicationService organizationApplicationService) {
        this.organizationApplicationService = organizationApplicationService;
    }

    /// 系部与专业写接口 ///

    /**
     * 添加系部
     *
     * @param request 添加系部请求
     * @return 新添加的系部 id
     */
    @SentinelRateLimit(resource = "organization.dept.add")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/add/dept")
    public BaseResponse<Long> addDept(@RequestBody DeptAddRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, organizationApplicationService.addDept(request));
    }

    /**
     * 添加专业
     *
     * @param request 添加专业请求
     * @return 新添加的专业 id
     */
    @SentinelRateLimit(resource = "organization.project.add")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/add/project")
    public BaseResponse<Long> addProject(@RequestBody ProjectAddRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, organizationApplicationService.addProject(request));
    }

    /**
     * 配置专业所属选题组
     *
     * @param request 专业选题组更新请求
     * @return 是否更新成功
     */
    @SentinelRateLimit(resource = "organization.project.update-group")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/update/project/group")
    public BaseResponse<Boolean> updateProjectGroup(@RequestBody ProjectGroupUpdateRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, organizationApplicationService.updateProjectGroup(request));
    }

    /**
     * 删除系部
     *
     * @param request 删除系部请求
     * @return 是否删除成功
     */
    @SentinelRateLimit(resource = "organization.dept.delete")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/delete/dept")
    public BaseResponse<Boolean> deleteDept(@RequestBody DeleteDeptRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, organizationApplicationService.deleteDept(request));
    }

    /**
     * 删除专业
     *
     * @param request 删除专业请求
     * @return 是否删除成功
     */
    @SentinelRateLimit(resource = "organization.project.delete")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/delete/project")
    public BaseResponse<Boolean> deleteProject(@RequestBody DeleteProjectRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, organizationApplicationService.deleteProject(request));
    }

    /// 系部与专业读接口 ///

    /**
     * 获取系部分页数据
     *
     * @param request 系部分页查询请求
     * @return 系部分页数据
     */
    @SentinelRateLimit(resource = "organization.dept.query-page")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/get/dept/page")
    public BaseResponse<Page<Dept>> getDept(@RequestBody DeptQueryRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, organizationApplicationService.getDeptPage(request));
    }

    /**
     * 获取系部列表数据（非管理员只能获取和当前登陆用户系部相同的系部）
     *
     * @param request 系部查询请求
     * @return 系部下拉列表数据
     */
    @SentinelRateLimit(resource = "organization.dept.query-list")
    @SaCheckLogin
    @PostMapping("/get/dept/list")
    public BaseResponse<List<DeptVO>> getDeptList(@RequestBody DeptQueryRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, organizationApplicationService.getDeptList(request));
    }

    /**
     * 获取专业分页数据
     *
     * @param request 专业分页查询请求
     * @return 专业分页数据
     */
    @SentinelRateLimit(resource = "organization.project.query-page")
    @SaCheckLogin
    @PostMapping("/get/project/page")
    public BaseResponse<Page<Project>> getProject(@RequestBody ProjectQueryRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, organizationApplicationService.getProjectPage(request));
    }

    /**
     * 获取专业列表数据（非管理员只能获取和当前登陆用户系部相同的系部）
     *
     * @param request 专业查询请求
     * @return 专业下拉列表数据
     */
    @SentinelRateLimit(resource = "organization.project.query-list")
    @SaCheckLogin
    @PostMapping("/get/project/list")
    public BaseResponse<List<ProjectVO>> getProjectList(@RequestBody ProjectQueryRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, organizationApplicationService.getProjectList(request));
    }

}
