package cn.com.edtechhub.worktopicselection.controller;

import cn.com.edtechhub.worktopicselection.annotation.SentinelRateLimit;
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
import cn.com.edtechhub.worktopicselection.response.BaseResponse;
import cn.com.edtechhub.worktopicselection.service.OrganizationApplicationService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 组织与选题组控制器接口契约单元测试
 *
 * @author wobushi041
 */
@ExtendWith(MockitoExtension.class)
class OrganizationAndGroupControllerContractTest {

    /**
     * 模拟组织与选题组应用服务依赖
     */
    @Mock
    private OrganizationApplicationService organizationApplicationService;

    /**
     * 待测组织管理控制器实例
     */
    @InjectMocks
    private OrganizationController organizationController;

    /**
     * 待测教师选题组控制器实例
     */
    @InjectMocks
    private TeacherGroupController teacherGroupController;

    // 场景：测试 ORG-001 ~ ORG-009 接口鉴权与限流注解完整性
    @Test
    void organizationEndpoints_shouldDeclareAuthAndRateLimitAnnotations() throws Exception {
        // 1. 准备测试数据
        Method addDeptMethod = OrganizationController.class.getMethod("addDept", DeptAddRequest.class);
        Method deleteDeptMethod = OrganizationController.class.getMethod("deleteDept", DeleteDeptRequest.class);
        Method getDeptMethod = OrganizationController.class.getMethod("getDept", DeptQueryRequest.class);
        Method getDeptListMethod = OrganizationController.class.getMethod("getDeptList", DeptQueryRequest.class);
        Method addProjectMethod = OrganizationController.class.getMethod("addProject", ProjectAddRequest.class);
        Method deleteProjectMethod = OrganizationController.class.getMethod("deleteProject", DeleteProjectRequest.class);
        Method updateProjectGroupMethod = OrganizationController.class.getMethod("updateProjectGroup", ProjectGroupUpdateRequest.class);
        Method getProjectMethod = OrganizationController.class.getMethod("getProject", ProjectQueryRequest.class);
        Method getProjectListMethod = OrganizationController.class.getMethod("getProjectList", ProjectQueryRequest.class);

        // 2. 调用反射获取方法注解
        SentinelRateLimit addDeptLimit = addDeptMethod.getAnnotation(SentinelRateLimit.class);
        SentinelRateLimit deleteDeptLimit = deleteDeptMethod.getAnnotation(SentinelRateLimit.class);
        SentinelRateLimit getDeptLimit = getDeptMethod.getAnnotation(SentinelRateLimit.class);
        SentinelRateLimit getDeptListLimit = getDeptListMethod.getAnnotation(SentinelRateLimit.class);
        SentinelRateLimit addProjectLimit = addProjectMethod.getAnnotation(SentinelRateLimit.class);
        SentinelRateLimit deleteProjectLimit = deleteProjectMethod.getAnnotation(SentinelRateLimit.class);
        SentinelRateLimit updateProjectGroupLimit = updateProjectGroupMethod.getAnnotation(SentinelRateLimit.class);
        SentinelRateLimit getProjectLimit = getProjectMethod.getAnnotation(SentinelRateLimit.class);
        SentinelRateLimit getProjectListLimit = getProjectListMethod.getAnnotation(SentinelRateLimit.class);

        // 3. 断言所有端点均声明 @SaCheckLogin 与对应的 @SentinelRateLimit 资源名
        assertNotNull(addDeptMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(deleteDeptMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(deleteDeptMethod.getAnnotation(SaCheckRole.class));
        assertNotNull(getDeptMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(getDeptListMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(addProjectMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(deleteProjectMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(deleteProjectMethod.getAnnotation(SaCheckRole.class));
        assertNotNull(updateProjectGroupMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(getProjectMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(getProjectListMethod.getAnnotation(SaCheckLogin.class));
        assertEquals("organization.dept.add", addDeptLimit.resource());
        assertEquals("organization.dept.delete", deleteDeptLimit.resource());
        assertEquals("organization.dept.query-page", getDeptLimit.resource());
        assertEquals("organization.dept.query-list", getDeptListLimit.resource());
        assertEquals("organization.project.add", addProjectLimit.resource());
        assertEquals("organization.project.delete", deleteProjectLimit.resource());
        assertEquals("organization.project.update-group", updateProjectGroupLimit.resource());
        assertEquals("organization.project.query-page", getProjectLimit.resource());
        assertEquals("organization.project.query-list", getProjectListLimit.resource());
    }

    // 场景：测试 GRP-001 ~ GRP-003 接口鉴权与限流注解完整性
    @Test
    void teacherGroupEndpoints_shouldDeclareAuthAndRateLimitAnnotations() throws Exception {
        // 1. 准备测试数据
        Method getTeacherGroupsMethod = TeacherGroupController.class.getMethod("getTeacherGroups");
        Method getTeacherGroupsBatchMethod = TeacherGroupController.class.getMethod("getTeacherGroupsBatch", TeacherGroupsBatchRequest.class);
        Method getGroupListMethod = TeacherGroupController.class.getMethod("getGroupList");

        // 2. 调用反射获取方法注解
        SentinelRateLimit groupsLimit = getTeacherGroupsMethod.getAnnotation(SentinelRateLimit.class);
        SentinelRateLimit groupsBatchLimit = getTeacherGroupsBatchMethod.getAnnotation(SentinelRateLimit.class);
        SentinelRateLimit groupListLimit = getGroupListMethod.getAnnotation(SentinelRateLimit.class);

        // 3. 断言所有端点均显式补齐 @SaCheckLogin、@SaCheckRole 与 @SentinelRateLimit
        assertNotNull(getTeacherGroupsMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(getTeacherGroupsMethod.getAnnotation(SaCheckRole.class));
        assertNotNull(getTeacherGroupsBatchMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(getTeacherGroupsBatchMethod.getAnnotation(SaCheckRole.class));
        assertNotNull(getGroupListMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(getGroupListMethod.getAnnotation(SaCheckRole.class));
        assertEquals("teacher-group.query-self", groupsLimit.resource());
        assertEquals("teacher-group.query-batch", groupsBatchLimit.resource());
        assertEquals("teacher-group.query-all", groupListLimit.resource());
    }

    // 场景：测试 OrganizationController 各端点正确委托给 OrganizationApplicationService
    @Test
    void organizationEndpoints_shouldDelegateToApplicationService() {
        // 1. 准备测试数据
        DeptAddRequest deptAddRequest = new DeptAddRequest();
        DeleteDeptRequest deleteDeptRequest = new DeleteDeptRequest();
        DeptQueryRequest deptQueryRequest = new DeptQueryRequest();
        ProjectAddRequest projectAddRequest = new ProjectAddRequest();
        DeleteProjectRequest deleteProjectRequest = new DeleteProjectRequest();
        ProjectGroupUpdateRequest groupUpdateRequest = new ProjectGroupUpdateRequest();
        ProjectQueryRequest projectQueryRequest = new ProjectQueryRequest();

        when(organizationApplicationService.addDept(deptAddRequest)).thenReturn(1L);
        when(organizationApplicationService.deleteDept(deleteDeptRequest)).thenReturn(true);
        when(organizationApplicationService.getDeptPage(deptQueryRequest)).thenReturn(new Page<Dept>());
        when(organizationApplicationService.getDeptList(deptQueryRequest)).thenReturn(Collections.singletonList(new DeptVO()));
        when(organizationApplicationService.addProject(projectAddRequest)).thenReturn(2L);
        when(organizationApplicationService.deleteProject(deleteProjectRequest)).thenReturn(true);
        when(organizationApplicationService.updateProjectGroup(groupUpdateRequest)).thenReturn(true);
        when(organizationApplicationService.getProjectPage(projectQueryRequest)).thenReturn(new Page<Project>());
        when(organizationApplicationService.getProjectList(projectQueryRequest)).thenReturn(Collections.singletonList(new ProjectVO()));

        // 2. 调用控制器各方法
        BaseResponse<Long> addDeptRes = organizationController.addDept(deptAddRequest);
        BaseResponse<Boolean> deleteDeptRes = organizationController.deleteDept(deleteDeptRequest);
        BaseResponse<Page<Dept>> pageDeptRes = organizationController.getDept(deptQueryRequest);
        BaseResponse<List<DeptVO>> listDeptRes = organizationController.getDeptList(deptQueryRequest);
        BaseResponse<Long> addProjectRes = organizationController.addProject(projectAddRequest);
        BaseResponse<Boolean> deleteProjectRes = organizationController.deleteProject(deleteProjectRequest);
        BaseResponse<Boolean> updateGroupRes = organizationController.updateProjectGroup(groupUpdateRequest);
        BaseResponse<Page<Project>> pageProjectRes = organizationController.getProject(projectQueryRequest);
        BaseResponse<List<ProjectVO>> listProjectRes = organizationController.getProjectList(projectQueryRequest);

        // 3. 断言响应体字段与服务调用次数正确
        assertEquals(1L, addDeptRes.getData());
        assertTrue(deleteDeptRes.getData());
        assertEquals(0, pageDeptRes.getCode());
        assertEquals(1, listDeptRes.getData().size());
        assertEquals(2L, addProjectRes.getData());
        assertTrue(deleteProjectRes.getData());
        assertTrue(updateGroupRes.getData());
        assertEquals(0, pageProjectRes.getCode());
        assertEquals(1, listProjectRes.getData().size());
        verify(organizationApplicationService).addDept(deptAddRequest);
        verify(organizationApplicationService).deleteDept(deleteDeptRequest);
        verify(organizationApplicationService).getDeptPage(deptQueryRequest);
        verify(organizationApplicationService).getDeptList(deptQueryRequest);
        verify(organizationApplicationService).addProject(projectAddRequest);
        verify(organizationApplicationService).deleteProject(deleteProjectRequest);
        verify(organizationApplicationService).updateProjectGroup(groupUpdateRequest);
        verify(organizationApplicationService).getProjectPage(projectQueryRequest);
        verify(organizationApplicationService).getProjectList(projectQueryRequest);
    }

    // 场景：测试 TeacherGroupController 各端点正确委托给 OrganizationApplicationService
    @Test
    void teacherGroupEndpoints_shouldDelegateToApplicationService() {
        // 1. 准备测试数据
        TeacherGroupsBatchRequest batchRequest = new TeacherGroupsBatchRequest();
        when(organizationApplicationService.getTeacherGroups()).thenReturn(Collections.emptyList());
        when(organizationApplicationService.getTeacherGroupsBatch(batchRequest)).thenReturn(Collections.emptyMap());
        when(organizationApplicationService.getGroupList()).thenReturn(Collections.singletonList("软件组"));

        // 2. 调用控制器各方法
        BaseResponse<List<Map<String, Object>>> selfRes = teacherGroupController.getTeacherGroups();
        BaseResponse<Map<String, List<Map<String, Object>>>> batchRes = teacherGroupController.getTeacherGroupsBatch(batchRequest);
        BaseResponse<List<String>> listRes = teacherGroupController.getGroupList();

        // 3. 断言响应体字段与服务调用次数正确
        assertEquals(0, selfRes.getCode());
        assertEquals(0, batchRes.getCode());
        assertEquals(Collections.singletonList("软件组"), listRes.getData());
        verify(organizationApplicationService).getTeacherGroups();
        verify(organizationApplicationService).getTeacherGroupsBatch(batchRequest);
        verify(organizationApplicationService).getGroupList();
    }

}
