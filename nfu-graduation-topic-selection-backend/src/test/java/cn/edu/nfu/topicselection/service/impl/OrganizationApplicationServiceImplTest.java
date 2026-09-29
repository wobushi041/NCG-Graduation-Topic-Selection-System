package cn.edu.nfu.topicselection.service.impl;

import cn.edu.nfu.topicselection.exception.BusinessException;
import cn.edu.nfu.topicselection.model.entity.Major;
import cn.edu.nfu.topicselection.model.entity.College;
import cn.edu.nfu.topicselection.model.entity.TopicGroup;
import cn.edu.nfu.topicselection.model.entity.User;
import cn.edu.nfu.topicselection.model.enums.UserRoleEnum;
import cn.edu.nfu.topicselection.model.request.organization.DeleteCollegeRequest;
import cn.edu.nfu.topicselection.model.request.organization.DeleteMajorRequest;
import cn.edu.nfu.topicselection.model.request.organization.CollegeQueryRequest;
import cn.edu.nfu.topicselection.model.request.organization.MajorGroupUpdateRequest;
import cn.edu.nfu.topicselection.model.request.organization.TeacherGroupQuotaUpdateRequest;
import cn.edu.nfu.topicselection.model.request.organization.TeacherGroupsBatchRequest;
import cn.edu.nfu.topicselection.service.CollegeService;
import cn.edu.nfu.topicselection.service.MajorService;
import cn.edu.nfu.topicselection.service.TeacherGroupService;
import cn.edu.nfu.topicselection.service.TopicGroupService;
import cn.edu.nfu.topicselection.service.TopicService;
import cn.edu.nfu.topicselection.service.UserService;
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
    private CollegeService collegeService;

    /**
     * 模拟专业服务依赖
     */
    @Mock
    private MajorService majorService;

    /**
     * 模拟选题组服务依赖
     */
    @Mock
    private TopicGroupService topicGroupService;

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
        Method deleteCollegeMethod = OrganizationApplicationServiceImpl.class.getMethod("deleteCollege", DeleteCollegeRequest.class);
        Method deleteMajorMethod = OrganizationApplicationServiceImpl.class.getMethod("deleteMajor", DeleteMajorRequest.class);
        Method getCollegeListMethod = OrganizationApplicationServiceImpl.class.getMethod("getCollegeList", CollegeQueryRequest.class);
        Method getTeacherGroupsMethod = OrganizationApplicationServiceImpl.class.getMethod("getTeacherGroups");

        // 2. 调用反射检查类与方法注解
        Transactional classTx = OrganizationApplicationServiceImpl.class.getAnnotation(Transactional.class);
        Transactional deleteCollegeTx = deleteCollegeMethod.getAnnotation(Transactional.class);
        Transactional deleteMajorTx = deleteMajorMethod.getAnnotation(Transactional.class);
        Transactional getCollegeListTx = getCollegeListMethod.getAnnotation(Transactional.class);
        Transactional getTeacherGroupsTx = getTeacherGroupsMethod.getAnnotation(Transactional.class);

        // 3. 断言写方法包含 @Transactional 而类级别与只读方法无 @Transactional
        assertNull(classTx);
        assertNotNull(deleteCollegeTx);
        assertNotNull(deleteMajorTx);
        assertNull(getCollegeListTx);
        assertNull(getTeacherGroupsTx);
    }

    // 场景：测试删除系部时若存在关联专业则拒绝删除并抛出异常
    @Test
    void deleteCollege_shouldThrowWhenAssociatedMajorsExist() {
        // 1. 准备测试数据
        DeleteCollegeRequest request = new DeleteCollegeRequest();
        request.setCollegeId(1L);
        College college = new College();
        college.setId(1L);
        when(collegeService.getById(1L)).thenReturn(college);
        when(majorService.count(any(QueryWrapper.class))).thenReturn(1L);

        // 2. 调用 deleteCollege 方法并捕获异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> organizationApplicationService.deleteCollege(request));

        // 3. 断言错误文案与存量逻辑一致
        assertEquals("请先删除学院下的所有专业", exception.getMessage());
    }

    // 场景：测试删除专业时若存在关联用户则拒绝删除并抛出异常
    @Test
    void deleteMajor_shouldThrowWhenAssociatedUsersExist() {
        // 1. 准备测试数据
        DeleteMajorRequest request = new DeleteMajorRequest();
        request.setMajorId(1L);
        when(majorService.getById(1L)).thenReturn(new Major());
        when(userService.count(any(QueryWrapper.class))).thenReturn(1L);

        // 2. 调用 deleteMajor 方法并捕获异常
        BusinessException exception = assertThrows(BusinessException.class,
                () -> organizationApplicationService.deleteMajor(request));

        // 3. 断言错误文案与存量逻辑一致
        assertEquals("请先删除该专业关联的用户", exception.getMessage());
    }

    // 场景：测试修改专业所属选题组成功时清除前后空格并更新
    @Test
    void updateMajorGroup_shouldTrimGroupNameAndUpdate() {
        // 1. 准备测试数据
        MajorGroupUpdateRequest request = new MajorGroupUpdateRequest();
        request.setMajorId(5L);
        request.setTopicGroupId(8L);

        Major major = new Major();
        major.setId(5L);
        major.setMajorName("软件工程");
        major.setCollegeId(1L);

        TopicGroup topicGroup = new TopicGroup();
        topicGroup.setId(8L);
        topicGroup.setCollegeId(1L);

        when(majorService.getById(5L)).thenReturn(major);
        when(topicGroupService.getById(8L)).thenReturn(topicGroup);
        when(majorService.updateById(major)).thenReturn(true);

        // 2. 调用 updateMajorGroup 方法
        Boolean result = organizationApplicationService.updateMajorGroup(request);

        // 3. 断言返回结果与更新字段正确
        assertTrue(result);
        assertEquals(8L, major.getTopicGroupId());
        verify(majorService).updateById(major);
    }

    // 场景：测试系部主任批量查询教师选题组额度时仅保留本系部教师账号
    @Test
    void getTeacherGroupsBatch_shouldFilterAccountsForCollegeUser() {
        // 1. 准备测试数据
        TeacherGroupsBatchRequest request = new TeacherGroupsBatchRequest();
        request.setTeacherAccounts(Arrays.asList("T001", "T002"));

        User collegeUser = new User();
        collegeUser.setUserRole(UserRoleEnum.TOPIC_LEADER.getCode());
        collegeUser.setCollegeId(1L);
        collegeUser.setTopicGroupId(8L);

        User allowedTeacher = new User();
        allowedTeacher.setUserAccount("T001");
        allowedTeacher.setCollegeId(1L);

        when(userService.userGetCurrentLoginUser()).thenReturn(collegeUser);
        when(userService.userIsTopicLeader(collegeUser)).thenReturn(true);
        when(teacherGroupService.teacherAccountsForGroup(8L)).thenReturn(Collections.singletonList("T001"));
        when(teacherGroupService.groupsBatch(Collections.singletonList("T001")))
                .thenReturn(Collections.singletonMap("T001", new java.util.ArrayList<>()));

        // 2. 调用 getTeacherGroupsBatch 方法
        Map<String, List<Map<String, Object>>> result = organizationApplicationService.getTeacherGroupsBatch(request);

        // 3. 断言仅本系部教师账号传入 teacherGroupService
        assertEquals(1, result.size());
        verify(teacherGroupService).groupsBatch(Collections.singletonList("T001"));
    }

    // 场景：测试管理员修改同一学院教师的选题组额度
    @Test
    void updateTeacherGroupQuotaShouldValidateOwnershipAndDelegateUpdate() {
        // 1. 准备同一学院的教师、选题组和额度更新请求
        TeacherGroupQuotaUpdateRequest request = new TeacherGroupQuotaUpdateRequest();
        request.setTeacherAccount("T001");
        request.setTopicGroupId(8L);
        request.setMaxTopics(10);
        User teacher = new User();
        teacher.setUserAccount("T001");
        teacher.setCollegeId(1L);
        TopicGroup topicGroup = new TopicGroup();
        topicGroup.setId(8L);
        topicGroup.setCollegeId(1L);
        when(userService.getOne(any(QueryWrapper.class))).thenReturn(teacher);
        when(userService.userIsTeacher(teacher)).thenReturn(true);
        when(topicGroupService.getById(8L)).thenReturn(topicGroup);

        // 2. 调用更新教师选题组额度方法
        Boolean result = organizationApplicationService.updateTeacherGroupQuota(request);

        // 3. 断言更新成功并向额度服务传递准确的教师、选题组和上限
        assertTrue(result);
        verify(teacherGroupService).updateQuota("T001", 8L, 10);
    }

}
