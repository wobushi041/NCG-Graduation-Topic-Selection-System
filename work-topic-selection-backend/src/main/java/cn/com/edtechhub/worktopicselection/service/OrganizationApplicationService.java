package cn.com.edtechhub.worktopicselection.service;

import cn.com.edtechhub.worktopicselection.model.entity.Dept;
import cn.com.edtechhub.worktopicselection.model.entity.Project;
import cn.com.edtechhub.worktopicselection.model.request.organization.DeleteDeptRequest;
import cn.com.edtechhub.worktopicselection.model.request.organization.DeleteProjectRequest;
import cn.com.edtechhub.worktopicselection.model.request.organization.DeptAddRequest;
import cn.com.edtechhub.worktopicselection.model.request.organization.DeptQueryRequest;
import cn.com.edtechhub.worktopicselection.model.request.organization.ProjectAddRequest;
import cn.com.edtechhub.worktopicselection.model.request.organization.ProjectGroupUpdateRequest;
import cn.com.edtechhub.worktopicselection.model.request.organization.ProjectQueryRequest;
import cn.com.edtechhub.worktopicselection.model.request.organization.TeacherGroupsBatchRequest;
import cn.com.edtechhub.worktopicselection.model.vo.DeptVO;
import cn.com.edtechhub.worktopicselection.model.vo.ProjectVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

/**
 * 定义系部维护、专业维护、专业选题组绑定以及教师选题组查询的应用服务契约
 *
 * @author wobushi041
 */
public interface OrganizationApplicationService {

    /**
     * 添加新的系部
     *
     * @param request 添加系部请求
     * @return 新添加的系部 id
     */
    Long addDept(DeptAddRequest request);

    /**
     * 添加新的专业并可选绑定所属选题组
     *
     * @param request 添加专业请求
     * @return 新添加的专业 id
     */
    Long addProject(ProjectAddRequest request);

    /**
     * 配置专业所属选题组
     *
     * @param request 更新专业选题组请求
     * @return 是否更新成功
     */
    Boolean updateProjectGroup(ProjectGroupUpdateRequest request);

    /**
     * 校验无下属专业、用户与课题后删除指定系部
     *
     * @param request 删除系部请求
     * @return 是否删除成功
     */
    Boolean deleteDept(DeleteDeptRequest request);

    /**
     * 校验无下属用户后删除指定专业
     *
     * @param request 删除专业请求
     * @return 是否删除成功
     */
    Boolean deleteProject(DeleteProjectRequest request);

    /**
     * 分页查询系部数据
     *
     * @param request 系部查询请求
     * @return 系部分页数据
     */
    Page<Dept> getDeptPage(DeptQueryRequest request);

    /**
     * 查询当前登录用户可见的系部下拉列表数据
     *
     * @param request 系部查询请求
     * @return 系部下拉列表数据
     */
    List<DeptVO> getDeptList(DeptQueryRequest request);

    /**
     * 分页查询专业数据
     *
     * @param request 专业查询请求
     * @return 专业分页数据
     */
    Page<Project> getProjectPage(ProjectQueryRequest request);

    /**
     * 查询专业下拉列表数据
     *
     * @param request 专业查询请求
     * @return 专业下拉列表数据
     */
    List<ProjectVO> getProjectList(ProjectQueryRequest request);

    /**
     * 获取当前登录教师的选题组及额度列表
     *
     * @return 当前教师的选题组信息列表
     */
    List<Map<String, Object>> getTeacherGroups();

    /**
     * 按角色范围过滤并批量查询指定教师的选题组及额度列表
     *
     * @param request 教师选题组额度批量查询请求
     * @return 教师账号到选题组额度列表的映射
     */
    Map<String, List<Map<String, Object>>> getTeacherGroupsBatch(TeacherGroupsBatchRequest request);

    /**
     * 查询系统内现有的全部选题组名称列表
     *
     * @return 系统内现有选题组名称列表
     */
    List<String> getGroupList();

}
