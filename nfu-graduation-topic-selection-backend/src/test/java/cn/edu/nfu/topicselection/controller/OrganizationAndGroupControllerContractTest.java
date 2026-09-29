package cn.edu.nfu.topicselection.controller;

import cn.edu.nfu.topicselection.annotation.SentinelRateLimit;
import cn.edu.nfu.topicselection.model.entity.College;
import cn.edu.nfu.topicselection.model.entity.Major;
import cn.edu.nfu.topicselection.model.request.organization.DeleteCollegeRequest;
import cn.edu.nfu.topicselection.model.request.organization.DeleteMajorRequest;
import cn.edu.nfu.topicselection.model.request.organization.CollegeAddRequest;
import cn.edu.nfu.topicselection.model.request.organization.CollegeQueryRequest;
import cn.edu.nfu.topicselection.model.request.organization.MajorAddRequest;
import cn.edu.nfu.topicselection.model.request.organization.MajorGroupUpdateRequest;
import cn.edu.nfu.topicselection.model.request.organization.MajorQueryRequest;
import cn.edu.nfu.topicselection.model.request.organization.TeacherGroupQuotaUpdateRequest;
import cn.edu.nfu.topicselection.model.request.organization.TeacherGroupsBatchRequest;
import cn.edu.nfu.topicselection.model.vo.CollegeVO;
import cn.edu.nfu.topicselection.model.vo.MajorVO;
import cn.edu.nfu.topicselection.response.BaseResponse;
import cn.edu.nfu.topicselection.service.OrganizationApplicationService;
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
        Method addCollegeMethod = OrganizationController.class.getMethod("addCollege", CollegeAddRequest.class);
        Method deleteCollegeMethod = OrganizationController.class.getMethod("deleteCollege", DeleteCollegeRequest.class);
        Method getCollegeMethod = OrganizationController.class.getMethod("getCollege", CollegeQueryRequest.class);
        Method getCollegeListMethod = OrganizationController.class.getMethod("getCollegeList", CollegeQueryRequest.class);
        Method addMajorMethod = OrganizationController.class.getMethod("addMajor", MajorAddRequest.class);
        Method deleteMajorMethod = OrganizationController.class.getMethod("deleteMajor", DeleteMajorRequest.class);
        Method updateMajorGroupMethod = OrganizationController.class.getMethod("updateMajorGroup", MajorGroupUpdateRequest.class);
        Method getMajorMethod = OrganizationController.class.getMethod("getMajor", MajorQueryRequest.class);
        Method getMajorListMethod = OrganizationController.class.getMethod("getMajorList", MajorQueryRequest.class);

        // 2. 调用反射获取方法注解
        SentinelRateLimit addCollegeLimit = addCollegeMethod.getAnnotation(SentinelRateLimit.class);
        SentinelRateLimit deleteCollegeLimit = deleteCollegeMethod.getAnnotation(SentinelRateLimit.class);
        SentinelRateLimit getCollegeLimit = getCollegeMethod.getAnnotation(SentinelRateLimit.class);
        SentinelRateLimit getCollegeListLimit = getCollegeListMethod.getAnnotation(SentinelRateLimit.class);
        SentinelRateLimit addMajorLimit = addMajorMethod.getAnnotation(SentinelRateLimit.class);
        SentinelRateLimit deleteMajorLimit = deleteMajorMethod.getAnnotation(SentinelRateLimit.class);
        SentinelRateLimit updateMajorGroupLimit = updateMajorGroupMethod.getAnnotation(SentinelRateLimit.class);
        SentinelRateLimit getMajorLimit = getMajorMethod.getAnnotation(SentinelRateLimit.class);
        SentinelRateLimit getMajorListLimit = getMajorListMethod.getAnnotation(SentinelRateLimit.class);

        // 3. 断言所有端点均声明 @SaCheckLogin 与对应的 @SentinelRateLimit 资源名
        assertNotNull(addCollegeMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(deleteCollegeMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(deleteCollegeMethod.getAnnotation(SaCheckRole.class));
        assertNotNull(getCollegeMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(getCollegeListMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(addMajorMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(deleteMajorMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(deleteMajorMethod.getAnnotation(SaCheckRole.class));
        assertNotNull(updateMajorGroupMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(getMajorMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(getMajorListMethod.getAnnotation(SaCheckLogin.class));
        assertEquals("organization.college.add", addCollegeLimit.resource());
        assertEquals("organization.college.delete", deleteCollegeLimit.resource());
        assertEquals("organization.college.query-page", getCollegeLimit.resource());
        assertEquals("organization.college.query-list", getCollegeListLimit.resource());
        assertEquals("organization.major.add", addMajorLimit.resource());
        assertEquals("organization.major.delete", deleteMajorLimit.resource());
        assertEquals("organization.major.update-group", updateMajorGroupLimit.resource());
        assertEquals("organization.major.query-page", getMajorLimit.resource());
        assertEquals("organization.major.query-list", getMajorListLimit.resource());
    }

    // 场景：测试 GRP-001 ~ GRP-003 接口鉴权与限流注解完整性
    @Test
    void teacherGroupEndpoints_shouldDeclareAuthAndRateLimitAnnotations() throws Exception {
        // 1. 准备测试数据
        Method getTeacherGroupsMethod = TeacherGroupController.class.getMethod("getTeacherGroups");
        Method getTeacherGroupsBatchMethod = TeacherGroupController.class.getMethod("getTeacherGroupsBatch", TeacherGroupsBatchRequest.class);
        Method updateTeacherGroupQuotaMethod = TeacherGroupController.class.getMethod(
                "updateTeacherGroupQuota", TeacherGroupQuotaUpdateRequest.class);
        Method getGroupListMethod = TeacherGroupController.class.getMethod("getGroupList");

        // 2. 调用反射获取方法注解
        SentinelRateLimit groupsLimit = getTeacherGroupsMethod.getAnnotation(SentinelRateLimit.class);
        SentinelRateLimit groupsBatchLimit = getTeacherGroupsBatchMethod.getAnnotation(SentinelRateLimit.class);
        SentinelRateLimit quotaUpdateLimit = updateTeacherGroupQuotaMethod.getAnnotation(SentinelRateLimit.class);
        SentinelRateLimit groupListLimit = getGroupListMethod.getAnnotation(SentinelRateLimit.class);

        // 3. 断言所有端点均显式补齐 @SaCheckLogin、@SaCheckRole 与 @SentinelRateLimit
        assertNotNull(getTeacherGroupsMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(getTeacherGroupsMethod.getAnnotation(SaCheckRole.class));
        assertNotNull(getTeacherGroupsBatchMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(getTeacherGroupsBatchMethod.getAnnotation(SaCheckRole.class));
        assertNotNull(updateTeacherGroupQuotaMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(updateTeacherGroupQuotaMethod.getAnnotation(SaCheckRole.class));
        assertNotNull(getGroupListMethod.getAnnotation(SaCheckLogin.class));
        assertNotNull(getGroupListMethod.getAnnotation(SaCheckRole.class));
        assertEquals("teacher-group.query-self", groupsLimit.resource());
        assertEquals("teacher-group.query-batch", groupsBatchLimit.resource());
        assertEquals("teacher-group.quota.update", quotaUpdateLimit.resource());
        assertEquals("teacher-group.query-all", groupListLimit.resource());
    }

    // 场景：测试 OrganizationController 各端点正确委托给 OrganizationApplicationService
    @Test
    void organizationEndpoints_shouldDelegateToApplicationService() {
        // 1. 准备测试数据
        CollegeAddRequest collegeAddRequest = new CollegeAddRequest();
        DeleteCollegeRequest deleteCollegeRequest = new DeleteCollegeRequest();
        CollegeQueryRequest collegeQueryRequest = new CollegeQueryRequest();
        MajorAddRequest majorAddRequest = new MajorAddRequest();
        DeleteMajorRequest deleteMajorRequest = new DeleteMajorRequest();
        MajorGroupUpdateRequest groupUpdateRequest = new MajorGroupUpdateRequest();
        MajorQueryRequest majorQueryRequest = new MajorQueryRequest();

        when(organizationApplicationService.addCollege(collegeAddRequest)).thenReturn(1L);
        when(organizationApplicationService.deleteCollege(deleteCollegeRequest)).thenReturn(true);
        when(organizationApplicationService.getCollegePage(collegeQueryRequest)).thenReturn(new Page<College>());
        when(organizationApplicationService.getCollegeList(collegeQueryRequest)).thenReturn(Collections.singletonList(new CollegeVO()));
        when(organizationApplicationService.addMajor(majorAddRequest)).thenReturn(2L);
        when(organizationApplicationService.deleteMajor(deleteMajorRequest)).thenReturn(true);
        when(organizationApplicationService.updateMajorGroup(groupUpdateRequest)).thenReturn(true);
        when(organizationApplicationService.getMajorPage(majorQueryRequest)).thenReturn(new Page<Major>());
        when(organizationApplicationService.getMajorList(majorQueryRequest)).thenReturn(Collections.singletonList(new MajorVO()));

        // 2. 调用控制器各方法
        BaseResponse<Long> addCollegeRes = organizationController.addCollege(collegeAddRequest);
        BaseResponse<Boolean> deleteCollegeRes = organizationController.deleteCollege(deleteCollegeRequest);
        BaseResponse<Page<College>> pageCollegeRes = organizationController.getCollege(collegeQueryRequest);
        BaseResponse<List<CollegeVO>> listCollegeRes = organizationController.getCollegeList(collegeQueryRequest);
        BaseResponse<Long> addMajorRes = organizationController.addMajor(majorAddRequest);
        BaseResponse<Boolean> deleteMajorRes = organizationController.deleteMajor(deleteMajorRequest);
        BaseResponse<Boolean> updateGroupRes = organizationController.updateMajorGroup(groupUpdateRequest);
        BaseResponse<Page<Major>> pageMajorRes = organizationController.getMajor(majorQueryRequest);
        BaseResponse<List<MajorVO>> listMajorRes = organizationController.getMajorList(majorQueryRequest);

        // 3. 断言响应体字段与服务调用次数正确
        assertEquals(1L, addCollegeRes.getData());
        assertTrue(deleteCollegeRes.getData());
        assertEquals(0, pageCollegeRes.getCode());
        assertEquals(1, listCollegeRes.getData().size());
        assertEquals(2L, addMajorRes.getData());
        assertTrue(deleteMajorRes.getData());
        assertTrue(updateGroupRes.getData());
        assertEquals(0, pageMajorRes.getCode());
        assertEquals(1, listMajorRes.getData().size());
        verify(organizationApplicationService).addCollege(collegeAddRequest);
        verify(organizationApplicationService).deleteCollege(deleteCollegeRequest);
        verify(organizationApplicationService).getCollegePage(collegeQueryRequest);
        verify(organizationApplicationService).getCollegeList(collegeQueryRequest);
        verify(organizationApplicationService).addMajor(majorAddRequest);
        verify(organizationApplicationService).deleteMajor(deleteMajorRequest);
        verify(organizationApplicationService).updateMajorGroup(groupUpdateRequest);
        verify(organizationApplicationService).getMajorPage(majorQueryRequest);
        verify(organizationApplicationService).getMajorList(majorQueryRequest);
    }

    // 场景：测试 TeacherGroupController 各端点正确委托给 OrganizationApplicationService
    @Test
    void teacherGroupEndpoints_shouldDelegateToApplicationService() {
        // 1. 准备测试数据
        TeacherGroupsBatchRequest batchRequest = new TeacherGroupsBatchRequest();
        TeacherGroupQuotaUpdateRequest updateRequest = new TeacherGroupQuotaUpdateRequest();
        when(organizationApplicationService.getTeacherGroups()).thenReturn(Collections.emptyList());
        when(organizationApplicationService.getTeacherGroupsBatch(batchRequest)).thenReturn(Collections.emptyMap());
        when(organizationApplicationService.updateTeacherGroupQuota(updateRequest)).thenReturn(true);
        when(organizationApplicationService.getGroupList()).thenReturn(Collections.singletonList("软件组"));

        // 2. 调用控制器各方法
        BaseResponse<List<Map<String, Object>>> selfRes = teacherGroupController.getTeacherGroups();
        BaseResponse<Map<String, List<Map<String, Object>>>> batchRes = teacherGroupController.getTeacherGroupsBatch(batchRequest);
        BaseResponse<Boolean> updateRes = teacherGroupController.updateTeacherGroupQuota(updateRequest);
        BaseResponse<List<String>> listRes = teacherGroupController.getGroupList();

        // 3. 断言响应体字段与服务调用次数正确
        assertEquals(0, selfRes.getCode());
        assertEquals(0, batchRes.getCode());
        assertTrue(updateRes.getData());
        assertEquals(Collections.singletonList("软件组"), listRes.getData());
        verify(organizationApplicationService).getTeacherGroups();
        verify(organizationApplicationService).getTeacherGroupsBatch(batchRequest);
        verify(organizationApplicationService).updateTeacherGroupQuota(updateRequest);
        verify(organizationApplicationService).getGroupList();
    }

}
