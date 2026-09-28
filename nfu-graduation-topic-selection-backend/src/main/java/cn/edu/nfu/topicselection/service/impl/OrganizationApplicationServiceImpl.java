package cn.edu.nfu.topicselection.service.impl;

import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import cn.edu.nfu.topicselection.model.entity.Dept;
import cn.edu.nfu.topicselection.model.entity.Project;
import cn.edu.nfu.topicselection.model.entity.Topic;
import cn.edu.nfu.topicselection.model.entity.User;
import cn.edu.nfu.topicselection.model.enums.UserRoleEnum;
import cn.edu.nfu.topicselection.model.request.organization.DeleteDeptRequest;
import cn.edu.nfu.topicselection.model.request.organization.DeleteProjectRequest;
import cn.edu.nfu.topicselection.model.request.organization.DeptAddRequest;
import cn.edu.nfu.topicselection.model.request.organization.DeptQueryRequest;
import cn.edu.nfu.topicselection.model.request.organization.ProjectAddRequest;
import cn.edu.nfu.topicselection.model.request.organization.ProjectGroupUpdateRequest;
import cn.edu.nfu.topicselection.model.request.organization.ProjectQueryRequest;
import cn.edu.nfu.topicselection.model.request.organization.TeacherGroupsBatchRequest;
import cn.edu.nfu.topicselection.model.vo.DeptVO;
import cn.edu.nfu.topicselection.model.vo.ProjectVO;
import cn.edu.nfu.topicselection.service.DeptService;
import cn.edu.nfu.topicselection.service.OrganizationApplicationService;
import cn.edu.nfu.topicselection.service.ProjectService;
import cn.edu.nfu.topicselection.service.TeacherGroupService;
import cn.edu.nfu.topicselection.service.TopicService;
import cn.edu.nfu.topicselection.service.UserService;
import cn.edu.nfu.topicselection.utils.ThrowUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于级联引用校验、方法级写事务与角色数据隔离实现组织（系部/专业）与教师选题组应用服务
 *
 * @author wobushi041
 */
@Service
public class OrganizationApplicationServiceImpl implements OrganizationApplicationService {

    /**
     * 注入系部服务依赖
     */
    private final DeptService deptService;

    /**
     * 注入专业服务依赖
     */
    private final ProjectService projectService;

    /**
     * 注入用户服务依赖
     */
    private final UserService userService;

    /**
     * 注入课题服务依赖
     */
    private final TopicService topicService;

    /**
     * 注入教师选题组服务依赖
     */
    private final TeacherGroupService teacherGroupService;

    /**
     * 初始化组织与教师选题组应用服务实现
     *
     * @param deptService         系部服务
     * @param projectService      专业服务
     * @param userService         用户服务
     * @param topicService        课题服务
     * @param teacherGroupService 教师选题组服务
     */
    public OrganizationApplicationServiceImpl(DeptService deptService, ProjectService projectService,
                                              UserService userService, TopicService topicService,
                                              TeacherGroupService teacherGroupService) {
        this.deptService = deptService;
        this.projectService = projectService;
        this.userService = userService;
        this.topicService = topicService;
        this.teacherGroupService = teacherGroupService;
    }

    /// 系部与专业写用例 ///

    /**
     * 校验系部名称非空与唯一性并在事务中保存新系部记录
     *
     * @param request 添加系部请求
     * @return 新添加的系部 id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addDept(DeptAddRequest request) {
        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        String deptName = request.getDeptName();
        ThrowUtils.throwIf(deptName == null, CodeBindMessageEnums.PARAMS_ERROR, "系部名称不能为空");

        Dept dept = deptService.getOne(new QueryWrapper<Dept>().eq("deptName", deptName));
        ThrowUtils.throwIf(dept != null, CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "该系部已存在, 请不要重复添加");

        // 添加新的系部
        Dept newDept = new Dept();
        newDept.setDeptName(deptName);
        boolean result = deptService.save(newDept);
        ThrowUtils.throwIf(!result, CodeBindMessageEnums.OPERATION_ERROR, "无法添加新的系部");
        return newDept.getId();
    }

    /**
     * 校验专业名称与系部名称非空及唯一性并在事务中保存新专业记录
     *
     * @param request 添加专业请求
     * @return 新添加的专业 id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addProject(ProjectAddRequest request) {
        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        String projectName = request.getProjectName();
        ThrowUtils.throwIf(projectName == null, CodeBindMessageEnums.PARAMS_ERROR, "专业名称不能为空");

        String deptName = request.getDeptName();
        ThrowUtils.throwIf(deptName == null, CodeBindMessageEnums.PARAMS_ERROR, "系部名称不能为空");

        Project project = projectService.getOne(new QueryWrapper<Project>().eq("projectName", projectName));
        ThrowUtils.throwIf(project != null, CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "该专业已存在, 请不要重复添加");

        // 添加新的专业
        Project newProject = new Project();
        newProject.setProjectName(projectName);
        newProject.setDeptName(deptName);
        newProject.setGroupName(StringUtils.trimToNull(request.getGroupName()));
        boolean result = projectService.save(newProject);
        ThrowUtils.throwIf(!result, CodeBindMessageEnums.OPERATION_ERROR, "无法添加新的专业");
        return newProject.getId();
    }

    /**
     * 校验专业存在性并在事务中更新其绑定的选题组名称
     *
     * @param request 更新专业选题组请求
     * @return 是否更新成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateProjectGroup(ProjectGroupUpdateRequest request) {
        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;
        String projectName = StringUtils.trimToNull(request.getProjectName());
        ThrowUtils.throwIf(projectName == null, CodeBindMessageEnums.PARAMS_ERROR, "专业名称不能为空");

        Project project = projectService.getOne(new QueryWrapper<Project>().eq("projectName", projectName));
        ThrowUtils.throwIf(project == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "专业不存在");

        project.setGroupName(StringUtils.trimToNull(request.getGroupName()));
        boolean updated = projectService.updateById(project);
        ThrowUtils.throwIf(!updated, CodeBindMessageEnums.OPERATION_ERROR, "无法保存专业选题组");
        return true;
    }

    /**
     * 依次检查系部下是否存在关联专业、用户和选题，确认无引用后在事务中删除系部
     *
     * @param request 删除系部请求
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteDept(DeleteDeptRequest request) {
        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        String deptName = request.getDeptName();
        ThrowUtils.throwIf(deptName == null, CodeBindMessageEnums.PARAMS_ERROR, "系部名称不能为空");

        // 保证先删除专业才能删除系部
        List<Project> projectList = projectService.list(new QueryWrapper<Project>().eq("deptName", deptName));
        if (!projectList.isEmpty()) {
            String projectNames = projectList.stream().map(Project::getProjectName).collect(Collectors.joining(", "));
            ThrowUtils.throwIf(true, CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "先删除属于该系部的所有专业（" + projectNames + "）后才能删除该系部");
        }

        // 保证先删除相关角色才能删除系部
        List<User> userList = userService.list(new QueryWrapper<User>().eq("dept", deptName));
        if (!userList.isEmpty()) {
            String userNames = userList.stream().limit(5).map(User::getUserName).collect(Collectors.joining(", "));
            if (userList.size() > 5) {
                userNames += "...";
            }
            ThrowUtils.throwIf(true, CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "先删除属于该系部的所有角色（" + userNames + "）后才能删除该系部");
        }

        // 保证先删除相关选题才能删除系部
        List<Topic> topicList = topicService.list(new QueryWrapper<Topic>().eq("deptName", deptName));
        if (!topicList.isEmpty()) {
            String topicNames = topicList.stream().limit(5).map(Topic::getTopic).collect(Collectors.joining(", "));
            if (topicList.size() > 5) {
                topicNames += "...";
            }
            ThrowUtils.throwIf(true, CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "先删除属于该系部的所有选题（" + topicNames + "）后才能删除该系部");
        }

        // 删除系部
        boolean resalt = deptService.remove(new QueryWrapper<Dept>().eq("deptName", deptName));
        ThrowUtils.throwIf(!resalt, CodeBindMessageEnums.NOT_FOUND_ERROR, "找不到该系部");
        return true;
    }

    /**
     * 检查专业下是否存在关联用户，确认无引用后在事务中删除专业
     *
     * @param request 删除专业请求
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteProject(DeleteProjectRequest request) {
        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        String projectName = request.getProjectName();
        ThrowUtils.throwIf(projectName == null, CodeBindMessageEnums.PARAMS_ERROR, "专业名称不能为空");

        // 保证先删除相关角色才能删除专业
        List<User> userList = userService.list(new QueryWrapper<User>().eq("project", projectName));
        if (!userList.isEmpty()) {
            String userNames = userList.stream().limit(5).map(User::getUserName).collect(Collectors.joining(", "));
            if (userList.size() > 5) {
                userNames += "...";
            }
            ThrowUtils.throwIf(true, CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "先删除属于该专业的所有角色（" + userNames + "）后才能删除该专业");
        }

        // 删除专业
        boolean resalt = projectService.remove(new QueryWrapper<Project>().eq("projectName", projectName));
        ThrowUtils.throwIf(!resalt, CodeBindMessageEnums.NOT_FOUND_ERROR, "找不到该专业");
        return true;
    }

    /// 系部、专业与选题组读用例 ///

    /**
     * 校验分页参数并调用 DeptService 执行分页查询
     *
     * @param request 系部查询请求
     * @return 系部分页数据
     */
    @Override
    public Page<Dept> getDeptPage(DeptQueryRequest request) {
        // 参数检查
        long current = request.getCurrent();
        ThrowUtils.throwIf(current < 1, CodeBindMessageEnums.PARAMS_ERROR, "页码号必须大于 0");

        long size = request.getPageSize();
        ThrowUtils.throwIf(size < 1 || size > 100, CodeBindMessageEnums.PARAMS_ERROR, "页大小必须在 1 到 100 之间");

        // 获取系部数据
        return deptService.page(new Page<>(current, size), deptService.getQueryWrapper(request));
    }

    /**
     * 按当前登录用户是否为管理员决定返回全部系部或仅返回同系部，并封装为 DeptVO 列表
     *
     * @param request 系部查询请求
     * @return 系部下拉列表数据
     */
    @Override
    public List<DeptVO> getDeptList(DeptQueryRequest request) {
        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");

        // 获取当前用户
        User user = userService.userGetCurrentLoginUser();

        // 获取当前用户的系部
        String deptName = user.getDept();

        // 查询所有 dept 列表
        List<Dept> deptList = deptService.list(userService.userIsAdmin(user) ? null : new QueryWrapper<Dept>().eq("deptName", deptName));

        // 脱敏数据
        List<DeptVO> deptVOList = new ArrayList<>();
        for (Dept dept : deptList) {
            DeptVO deptVO = new DeptVO();
            deptVO.setLabel(dept.getDeptName());
            deptVO.setValue(dept.getDeptName());
            deptVOList.add(deptVO);
        }
        return deptVOList;
    }

    /**
     * 校验分页参数并调用 ProjectService 执行分页查询
     *
     * @param request 专业查询请求
     * @return 专业分页数据
     */
    @Override
    public Page<Project> getProjectPage(ProjectQueryRequest request) {
        // 参数检查
        long current = request.getCurrent();
        ThrowUtils.throwIf(current < 1, CodeBindMessageEnums.PARAMS_ERROR, "页码号必须大于 0");

        long size = request.getPageSize();
        ThrowUtils.throwIf(size < 1 || size > 100, CodeBindMessageEnums.PARAMS_ERROR, "页大小必须在 1 到 100 之间");

        // 获取专业数据
        return projectService.page(new Page<>(current, size), projectService.getQueryWrapper(request));
    }

    /**
     * 校验分页参数、查询专业记录并封装为 ProjectVO 下拉列表
     *
     * @param request 专业查询请求
     * @return 专业下拉列表数据
     */
    @Override
    public List<ProjectVO> getProjectList(ProjectQueryRequest request) {
        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        long current = request.getCurrent();
        ThrowUtils.throwIf(current < 1, CodeBindMessageEnums.PARAMS_ERROR, "页码号必须大于 0");

        long size = request.getPageSize();
        ThrowUtils.throwIf(size < 1 || size > 100, CodeBindMessageEnums.PARAMS_ERROR, "页大小必须在 1 到 100 之间");

        // 获取专业数据
        Page<Project> projectPage = projectService.page(new Page<>(current, size), projectService.getQueryWrapper(request));
        List<ProjectVO> projectVOList = new ArrayList<>();
        for (Project project : projectPage.getRecords()) {
            final String projectName = project.getProjectName();
            final ProjectVO projectVO = new ProjectVO();
            projectVO.setLabel(projectName);
            projectVO.setValue(projectName);
            projectVOList.add(projectVO);
        }
        return projectVOList;
    }

    /**
     * 获取当前登录教师账号并委托 TeacherGroupService 查询其选题组及额度列表
     *
     * @return 当前教师的选题组信息列表
     */
    @Override
    public List<Map<String, Object>> getTeacherGroups() {
        return teacherGroupService.groups(userService.userGetCurrentLoginUser().getUserAccount());
    }

    /**
     * 校验请求账号列表并在系部主任登录时仅保留同系部教师账号后委托 TeacherGroupService 批量查询
     *
     * @param request 教师选题组额度批量查询请求
     * @return 教师账号到选题组额度列表的映射
     */
    @Override
    public Map<String, List<Map<String, Object>>> getTeacherGroupsBatch(TeacherGroupsBatchRequest request) {
        ThrowUtils.throwIf(request == null || request.getTeacherAccounts() == null,
                CodeBindMessageEnums.PARAMS_ERROR, "教师账号列表不能为空");
        List<String> accounts = request.getTeacherAccounts();
        User loginUser = userService.userGetCurrentLoginUser();
        if (Boolean.TRUE.equals(userService.userIsDept(loginUser))) {
            List<User> deptTeachers = userService.list(new QueryWrapper<User>()
                    .eq("userRole", UserRoleEnum.TEACHER.getCode())
                    .eq("dept", loginUser.getDept()));
            Set<String> allowedAccounts = deptTeachers.stream()
                    .map(User::getUserAccount)
                    .collect(Collectors.toSet());
            accounts = accounts.stream().filter(allowedAccounts::contains).collect(Collectors.toList());
        }
        return teacherGroupService.groupsBatch(accounts);
    }

    /**
     * 委托 TeacherGroupService 查询系统内现有的全部选题组名称列表
     *
     * @return 系统内现有选题组名称列表
     */
    @Override
    public List<String> getGroupList() {
        return teacherGroupService.allGroups();
    }

}
