package cn.com.edtechhub.worktopicselection.service.impl;

import cn.com.edtechhub.worktopicselection.exception.BusinessException;
import cn.com.edtechhub.worktopicselection.model.entity.Project;
import cn.com.edtechhub.worktopicselection.model.entity.User;
import cn.com.edtechhub.worktopicselection.model.enums.UserRoleEnum;
import cn.com.edtechhub.worktopicselection.model.request.organization.DeleteDeptRequest;
import cn.com.edtechhub.worktopicselection.model.request.organization.DeleteProjectRequest;
import cn.com.edtechhub.worktopicselection.model.request.organization.DeptQueryRequest;
import cn.com.edtechhub.worktopicselection.model.request.organization.ProjectGroupUpdateRequest;
import cn.com.edtechhub.worktopicselection.model.request.organization.TeacherGroupsBatchRequest;
import cn.com.edtechhub.worktopicselection.service.DeptService;
import cn.com.edtechhub.worktopicselection.service.ProjectService;
import cn.com.edtechhub.worktopicselection.service.TeacherGroupService;
import cn.com.edtechhub.worktopicselection.service.TopicService;
import cn.com.edtechhub.worktopicselection.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 组织与选题组应用服务实现类单元测试
 *
 * @author wobushi041
 */
@ExtendWith(MockitoExtension.class)
class OrganizationApplicationServiceImplTest {

    /**
     * 模拟系部服务依赖
     */
    @Mock
    private DeptService deptService;

    /**
     * 模拟专业服务依赖
     */
    @Mock
    private ProjectService projectService;

    /**
     * 模拟用户服务依赖
     */
    @Mock
    private UserService userService;

    /**
     * 模拟选题服务依赖
     */
    @Mock
    private TopicService topicService;

    /**
     * 模拟教师选题组服务依赖
     */
    @Mock
    private TeacherGroupService teacherGroupService;

    /**
     * 待测组织与选题组应用服务实现实例
     */
    @InjectMocks
    private OrganizationApplicationServiceImpl organizationApplicationService;

    // 场景：测试事务边界仅在写方法上使用 @Transactional 且类级别与只读方法不开启事务
    @Test
    void transactionBoundary_shouldOnlyAnnotateWriteMethods() throws Exception {
        // 1. 准备测试数据
        Method deleteDeptMethod = OrganizationApplicationServiceImpl.class.getMethod("deleteDept", DeleteDeptRequest.class);
        Method deleteProjectMethod = OrganizationApplicationServiceImpl.class.getMethod("deleteProject", DeleteProjectRequest.class);
        Method getDeptListMethod = OrganizationApplicationServiceImpl.class.getMethod("getDeptList", DeptQueryRequest.class);
        Method getTeacherGroupsMethod = OrganizationApplicationServiceImpl.class.getMethod("getTeacherGroups");

        // 2. 调用反射检查类与方法注解
        Transactional classTx = OrganizationApplicationServiceImpl.class.getAnnotation(Transactional.class);
        Transactional deleteDeptTx = deleteDeptMethod.getAnnotation(Transactional.class);
        Transactional deleteProjectTx = deleteProjectMethod.getAnnotation(Transactional.class);
        Transactional getDeptListTx = getDeptListMethod.getAnnotation(Transactional.class);
        Transactional getTeacherGroupsTx = getTeacherGroupsMethod.getAnnotation(Transactional.class);

        // 3. 断言写方法包含 @Transactional 而类级别与只读方法无 @Transactional
        assertNull(classTx);
        assertNotNull(deleteDeptTx);
        assertNotNull(deleteProjectTx);
        assertNull(getDeptListTx);
        assertNull(getTeacherGroupsTx);
    }

    // 场景：测试删除系部时若存在关联专业则拒绝删除并抛出异常
    @Test
    void deleteDept_shouldThrowWhenAssociatedProjectsExist() {
        // 1. 准备测试数据
        DeleteDeptRequest request = new DeleteDeptRequest();
        request.setDeptName("计算机工程系");

        Project project = new Project();
        project.setProjectName("软件工程");
        when(projectService.list(any(QueryWrapper.class))).thenReturn(Collections.singletonList(project));

        // 2. 调用 deleteDept 方法并捕获异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> organizationApplicationService.deleteDept(request));

        // 3. 断言错误文案与存量逻辑一致
        assertEquals("先删除属于该系部的所有专业（软件工程）后才能删除该系部", exception.getMessage());
    }

    // 场景：测试删除专业时若存在关联用户则拒绝删除并抛出异常
    @Test
    void deleteProject_shouldThrowWhenAssociatedUsersExist() {
        // 1. 准备测试数据
        DeleteProjectRequest request = new DeleteProjectRequest();
        request.setProjectName("软件工程");

        User user = new User();
        user.setUserName("张三");
        when(userService.list(any(QueryWrapper.class))).thenReturn(Collections.singletonList(user));

        // 2. 调用 deleteProject 方法并捕获异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> organizationApplicationService.deleteProject(request));

        // 3. 断言错误文案与存量逻辑一致
        assertEquals("先删除属于该专业的所有角色（张三）后才能删除该专业", exception.getMessage());
    }

    // 场景：测试修改专业所属选题组成功时清除前后空格并更新
    @Test
    void updateProjectGroup_shouldTrimGroupNameAndUpdate() {
        // 1. 准备测试数据
        ProjectGroupUpdateRequest request = new ProjectGroupUpdateRequest();
        request.setProjectName("软件工程");
        request.setGroupName("  软件组  ");

        Project project = new Project();
        project.setId(5L);
        project.setProjectName("软件工程");
        project.setDeptName("计算机工程系");

        when(projectService.getOne(any(QueryWrapper.class))).thenReturn(project);
        when(projectService.updateById(project)).thenReturn(true);

        // 2. 调用 updateProjectGroup 方法
        Boolean result = organizationApplicationService.updateProjectGroup(request);

        // 3. 断言返回结果与更新字段正确
        assertTrue(result);
        assertEquals("软件组", project.getGroupName());
        verify(projectService).updateById(project);
    }

    // 场景：测试系部主任批量查询教师选题组额度时仅保留本系部教师账号
    @Test
    void getTeacherGroupsBatch_shouldFilterAccountsForDeptUser() {
        // 1. 准备测试数据
        TeacherGroupsBatchRequest request = new TeacherGroupsBatchRequest();
        request.setTeacherAccounts(Arrays.asList("T001", "T002"));

        User deptUser = new User();
        deptUser.setUserRole(UserRoleEnum.DEPT.getCode());
        deptUser.setDept("计算机工程系");

        User allowedTeacher = new User();
        allowedTeacher.setUserAccount("T001");
        allowedTeacher.setDept("计算机工程系");

        when(userService.userGetCurrentLoginUser()).thenReturn(deptUser);
        when(userService.userIsDept(deptUser)).thenReturn(true);
        when(userService.list(any(QueryWrapper.class))).thenReturn(Collections.singletonList(allowedTeacher));
        when(teacherGroupService.groupsBatch(Collections.singletonList("T001")))
                .thenReturn(Collections.singletonMap("T001", Collections.emptyList()));

        // 2. 调用 getTeacherGroupsBatch 方法
        Map<String, List<Map<String, Object>>> result = organizationApplicationService.getTeacherGroupsBatch(request);

        // 3. 断言仅本系部教师账号传入 teacherGroupService
        assertEquals(1, result.size());
        verify(teacherGroupService).groupsBatch(Collections.singletonList("T001"));
    }

}
