package cn.edu.nfu.topicselection.controller;

import cn.edu.nfu.topicselection.annotation.SentinelRateLimit;
import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import cn.edu.nfu.topicselection.model.request.organization.TeacherGroupQuotaUpdateRequest;
import cn.edu.nfu.topicselection.model.request.organization.TeacherGroupsBatchRequest;
import cn.edu.nfu.topicselection.response.BaseResponse;
import cn.edu.nfu.topicselection.response.TheResult;
import cn.edu.nfu.topicselection.service.OrganizationApplicationService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 教师选题组及额度查询控制层
 *
 * @author wobushi041
 */
@RestController
@RequestMapping("/user")
public class TeacherGroupController {

    /**
     * 注入组织与教师选题组应用服务依赖
     */
    private final OrganizationApplicationService organizationApplicationService;

    /**
     * 初始化教师选题组控制层
     *
     * @param organizationApplicationService 组织与教师选题组应用服务
     */
    public TeacherGroupController(OrganizationApplicationService organizationApplicationService) {
        this.organizationApplicationService = organizationApplicationService;
    }

    /**
     * 获取当前登录教师的选题组及额度列表
     *
     * @return 当前教师的选题组信息列表
     */
    @SentinelRateLimit(resource = "teacher-group.query-self")
    @SaCheckLogin
    @SaCheckRole(value = {"teacher"}, mode = SaMode.OR)
    @GetMapping("/teacher/groups")
    public BaseResponse<List<Map<String, Object>>> getTeacherGroups() {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, organizationApplicationService.getTeacherGroups());
    }

    /**
     * 批量查询指定教师的选题组额度，供教师列表展示使用（管理员可查全部，选题负责人仅限本学院）
     *
     * @param request 批量查询教师选题组请求
     * @return 教师账号到选题组额度列表的映射
     */
    @SentinelRateLimit(resource = "teacher-group.query-batch")
    @SaCheckLogin
    @SaCheckRole(value = {"admin", "topic_leader"}, mode = SaMode.OR)
    @PostMapping("/teacher/groups/batch")
    public BaseResponse<Map<String, List<Map<String, Object>>>> getTeacherGroupsBatch(@RequestBody TeacherGroupsBatchRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, organizationApplicationService.getTeacherGroupsBatch(request));
    }

    /**
     * 修改教师在指定选题组中的最大出题数量
     *
     * @param request 教师选题组额度更新请求
     * @return 是否更新成功
     */
    @SentinelRateLimit(resource = "teacher-group.quota.update")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/teacher/group/quota")
    public BaseResponse<Boolean> updateTeacherGroupQuota(@RequestBody TeacherGroupQuotaUpdateRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS,
                organizationApplicationService.updateTeacherGroupQuota(request));
    }

    /**
     * 查询系统内现有的选题组名称列表，供管理员配置专业选题组、添加专业等下拉使用
     *
     * @return 系统内现有选题组名称列表
     */
    @SentinelRateLimit(resource = "teacher-group.query-all")
    @SaCheckLogin
    @SaCheckRole(value = {"admin", "topic_leader"}, mode = SaMode.OR)
    @GetMapping("/group/list")
    public BaseResponse<List<String>> getGroupList() {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, organizationApplicationService.getGroupList());
    }

}
