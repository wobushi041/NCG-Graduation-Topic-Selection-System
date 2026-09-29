package cn.edu.nfu.topicselection.controller;

import cn.edu.nfu.topicselection.annotation.SentinelRateLimit;
import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import cn.edu.nfu.topicselection.model.entity.College;
import cn.edu.nfu.topicselection.model.entity.Major;
import cn.edu.nfu.topicselection.model.entity.TopicGroup;
import cn.edu.nfu.topicselection.model.request.organization.DeleteCollegeRequest;
import cn.edu.nfu.topicselection.model.request.organization.DeleteMajorRequest;
import cn.edu.nfu.topicselection.model.request.organization.CollegeAddRequest;
import cn.edu.nfu.topicselection.model.request.organization.CollegeQueryRequest;
import cn.edu.nfu.topicselection.model.request.organization.MajorAddRequest;
import cn.edu.nfu.topicselection.model.request.organization.MajorGroupUpdateRequest;
import cn.edu.nfu.topicselection.model.request.organization.MajorQueryRequest;
import cn.edu.nfu.topicselection.model.request.organization.TopicGroupAddRequest;
import cn.edu.nfu.topicselection.model.request.organization.TopicGroupDeleteRequest;
import cn.edu.nfu.topicselection.model.request.organization.TopicGroupQueryRequest;
import cn.edu.nfu.topicselection.model.request.organization.TopicGroupUpdateRequest;
import cn.edu.nfu.topicselection.model.vo.CollegeVO;
import cn.edu.nfu.topicselection.model.vo.MajorVO;
import cn.edu.nfu.topicselection.model.vo.TopicGroupVO;
import cn.edu.nfu.topicselection.response.BaseResponse;
import cn.edu.nfu.topicselection.response.TheResult;
import cn.edu.nfu.topicselection.service.OrganizationApplicationService;
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
 * 学院、专业与选题组配置控制层
 *
 * @author wobushi041
 */
@RestController
@RequestMapping("/organization")
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

    /// 学院、专业与选题组写接口 ///

    /**
     * 添加学院
     *
     * @param request 添加学院请求
     * @return 新添加的学院 id
     */
    @SentinelRateLimit(resource = "organization.college.add")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/add/college")
    public BaseResponse<Long> addCollege(@RequestBody CollegeAddRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, organizationApplicationService.addCollege(request));
    }

    /**
     * 添加专业
     *
     * @param request 添加专业请求
     * @return 新添加的专业 id
     */
    @SentinelRateLimit(resource = "organization.major.add")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/add/major")
    public BaseResponse<Long> addMajor(@RequestBody MajorAddRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, organizationApplicationService.addMajor(request));
    }

    /**
     * 配置专业所属选题组
     *
     * @param request 专业选题组更新请求
     * @return 是否更新成功
     */
    @SentinelRateLimit(resource = "organization.major.update-group")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/update/major/group")
    public BaseResponse<Boolean> updateMajorGroup(@RequestBody MajorGroupUpdateRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, organizationApplicationService.updateMajorGroup(request));
    }

    /**
     * 添加选题组
     *
     * @param request 选题组创建请求
     * @return 新增选题组 id
     */
    @SentinelRateLimit(resource = "organization.topic-group.add")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/topic-group/add")
    public BaseResponse<Long> addTopicGroup(@RequestBody TopicGroupAddRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS,
                organizationApplicationService.addTopicGroup(request));
    }

    /**
     * 更新选题组
     *
     * @param request 选题组更新请求
     * @return 是否更新成功
     */
    @SentinelRateLimit(resource = "organization.topic-group.update")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/topic-group/update")
    public BaseResponse<Boolean> updateTopicGroup(@RequestBody TopicGroupUpdateRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS,
                organizationApplicationService.updateTopicGroup(request));
    }

    /**
     * 删除选题组
     *
     * @param request 选题组删除请求
     * @return 是否删除成功
     */
    @SentinelRateLimit(resource = "organization.topic-group.delete")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/topic-group/delete")
    public BaseResponse<Boolean> deleteTopicGroup(@RequestBody TopicGroupDeleteRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS,
                organizationApplicationService.deleteTopicGroup(request));
    }

    /**
     * 删除学院
     *
     * @param request 删除学院请求
     * @return 是否删除成功
     */
    @SentinelRateLimit(resource = "organization.college.delete")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/delete/college")
    public BaseResponse<Boolean> deleteCollege(@RequestBody DeleteCollegeRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, organizationApplicationService.deleteCollege(request));
    }

    /**
     * 删除专业
     *
     * @param request 删除专业请求
     * @return 是否删除成功
     */
    @SentinelRateLimit(resource = "organization.major.delete")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/delete/major")
    public BaseResponse<Boolean> deleteMajor(@RequestBody DeleteMajorRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, organizationApplicationService.deleteMajor(request));
    }

    /// 学院、专业与选题组读接口 ///

    /**
     * 获取学院分页数据
     *
     * @param request 学院分页查询请求
     * @return 学院分页数据
     */
    @SentinelRateLimit(resource = "organization.college.query-page")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/get/college/page")
    public BaseResponse<Page<College>> getCollege(@RequestBody CollegeQueryRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, organizationApplicationService.getCollegePage(request));
    }

    /**
     * 获取学院列表数据（非管理员只能获取和当前登陆用户学院相同的学院）
     *
     * @param request 学院查询请求
     * @return 学院下拉列表数据
     */
    @SentinelRateLimit(resource = "organization.college.query-list")
    @SaCheckLogin
    @PostMapping("/get/college/list")
    public BaseResponse<List<CollegeVO>> getCollegeList(@RequestBody CollegeQueryRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, organizationApplicationService.getCollegeList(request));
    }

    /**
     * 获取专业分页数据
     *
     * @param request 专业分页查询请求
     * @return 专业分页数据
     */
    @SentinelRateLimit(resource = "organization.major.query-page")
    @SaCheckLogin
    @PostMapping("/get/major/page")
    public BaseResponse<Page<Major>> getMajor(@RequestBody MajorQueryRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, organizationApplicationService.getMajorPage(request));
    }

    /**
     * 获取专业列表数据（非管理员只能获取和当前登陆用户学院相同的学院）
     *
     * @param request 专业查询请求
     * @return 专业下拉列表数据
     */
    @SentinelRateLimit(resource = "organization.major.query-list")
    @SaCheckLogin
    @PostMapping("/get/major/list")
    public BaseResponse<List<MajorVO>> getMajorList(@RequestBody MajorQueryRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, organizationApplicationService.getMajorList(request));
    }

    /**
     * 分页查询选题组
     *
     * @param request 选题组查询请求
     * @return 选题组分页数据
     */
    @SentinelRateLimit(resource = "organization.topic-group.query-page")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/topic-group/page")
    public BaseResponse<Page<TopicGroup>> getTopicGroupPage(@RequestBody TopicGroupQueryRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS,
                organizationApplicationService.getTopicGroupPage(request));
    }

    /**
     * 查询选题组选项
     *
     * @param request 选题组查询请求
     * @return 选题组选项
     */
    @SentinelRateLimit(resource = "organization.topic-group.query-list")
    @SaCheckLogin
    @PostMapping("/topic-group/list")
    public BaseResponse<List<TopicGroupVO>> getTopicGroupList(@RequestBody TopicGroupQueryRequest request) {
        return TheResult.success(CodeBindMessageEnums.SUCCESS,
                organizationApplicationService.getTopicGroupList(request));
    }

}
