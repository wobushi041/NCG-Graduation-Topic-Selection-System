package cn.edu.nfu.topicselection.service.impl;

import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import cn.edu.nfu.topicselection.model.entity.College;
import cn.edu.nfu.topicselection.model.entity.Major;
import cn.edu.nfu.topicselection.model.entity.Topic;
import cn.edu.nfu.topicselection.model.entity.TopicGroup;
import cn.edu.nfu.topicselection.model.entity.User;
import cn.edu.nfu.topicselection.model.request.organization.CollegeAddRequest;
import cn.edu.nfu.topicselection.model.request.organization.CollegeQueryRequest;
import cn.edu.nfu.topicselection.model.request.organization.DeleteCollegeRequest;
import cn.edu.nfu.topicselection.model.request.organization.DeleteMajorRequest;
import cn.edu.nfu.topicselection.model.request.organization.MajorAddRequest;
import cn.edu.nfu.topicselection.model.request.organization.MajorGroupUpdateRequest;
import cn.edu.nfu.topicselection.model.request.organization.MajorQueryRequest;
import cn.edu.nfu.topicselection.model.request.organization.TeacherGroupQuotaUpdateRequest;
import cn.edu.nfu.topicselection.model.request.organization.TeacherGroupsBatchRequest;
import cn.edu.nfu.topicselection.model.request.organization.TopicGroupAddRequest;
import cn.edu.nfu.topicselection.model.request.organization.TopicGroupDeleteRequest;
import cn.edu.nfu.topicselection.model.request.organization.TopicGroupQueryRequest;
import cn.edu.nfu.topicselection.model.request.organization.TopicGroupUpdateRequest;
import cn.edu.nfu.topicselection.model.vo.CollegeVO;
import cn.edu.nfu.topicselection.model.vo.MajorVO;
import cn.edu.nfu.topicselection.model.vo.TopicGroupVO;
import cn.edu.nfu.topicselection.service.CollegeService;
import cn.edu.nfu.topicselection.service.MajorService;
import cn.edu.nfu.topicselection.service.OrganizationApplicationService;
import cn.edu.nfu.topicselection.service.TeacherGroupService;
import cn.edu.nfu.topicselection.service.TopicGroupService;
import cn.edu.nfu.topicselection.service.TopicService;
import cn.edu.nfu.topicselection.service.UserService;
import cn.edu.nfu.topicselection.utils.ThrowUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于 ID 关联和引用校验实现学院、专业及选题组应用服务
 *
 * @author wobushi041
 */
@Service
public class OrganizationApplicationServiceImpl implements OrganizationApplicationService {

    /**
     * 注入学院服务依赖
     */
    private final CollegeService collegeService;

    /**
     * 注入专业服务依赖
     */
    private final MajorService majorService;

    /**
     * 注入选题组服务依赖
     */
    private final TopicGroupService topicGroupService;

    /**
     * 注入用户服务依赖
     */
    private final UserService userService;

    /**
     * 注入题目服务依赖
     */
    private final TopicService topicService;

    /**
     * 注入教师选题组额度服务依赖
     */
    private final TeacherGroupService teacherGroupService;

    /**
     * 初始化组织应用服务
     *
     * @param collegeService      学院服务
     * @param majorService        专业服务
     * @param topicGroupService   选题组服务
     * @param userService         用户服务
     * @param topicService        题目服务
     * @param teacherGroupService 教师选题组额度服务
     */
    public OrganizationApplicationServiceImpl(CollegeService collegeService, MajorService majorService,
                                              TopicGroupService topicGroupService, UserService userService,
                                              TopicService topicService, TeacherGroupService teacherGroupService) {
        this.collegeService = collegeService;
        this.majorService = majorService;
        this.topicGroupService = topicGroupService;
        this.userService = userService;
        this.topicService = topicService;
        this.teacherGroupService = teacherGroupService;
    }

    /// 组织写用例 ///

    /**
     * 校验学院名称唯一性后保存学院记录
     *
     * @param request 学院创建请求
     * @return 新增学院 id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addCollege(CollegeAddRequest request) {
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;
        String collegeName = StringUtils.trimToNull(request.getCollegeName());
        ThrowUtils.throwIf(collegeName == null, CodeBindMessageEnums.PARAMS_ERROR, "学院名称不能为空");
        ThrowUtils.throwIf(collegeService.count(new QueryWrapper<College>().eq("collegeName", collegeName)) > 0,
                CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "该学院已存在, 请不要重复添加");
        College college = new College();
        college.setCollegeName(collegeName);
        ThrowUtils.throwIf(!collegeService.save(college), CodeBindMessageEnums.OPERATION_ERROR, "无法添加新的学院");
        return college.getId();
    }

    /**
     * 校验学院和选题组关系后保存专业记录
     *
     * @param request 专业创建请求
     * @return 新增专业 id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addMajor(MajorAddRequest request) {
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;
        String majorName = StringUtils.trimToNull(request.getMajorName());
        ThrowUtils.throwIf(majorName == null || request.getCollegeId() == null || request.getTopicGroupId() == null,
                CodeBindMessageEnums.PARAMS_ERROR, "专业名称、学院和选题组不能为空");
        College college = requireCollege(request.getCollegeId());
        TopicGroup topicGroup = requireTopicGroup(request.getTopicGroupId());
        ThrowUtils.throwIf(!college.getId().equals(topicGroup.getCollegeId()),
                CodeBindMessageEnums.PARAMS_ERROR, "选题组不属于所选学院");
        ThrowUtils.throwIf(majorService.count(new QueryWrapper<Major>()
                        .eq("collegeId", college.getId()).eq("majorName", majorName)) > 0,
                CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "该专业已存在, 请不要重复添加");
        Major major = new Major();
        major.setMajorName(majorName);
        major.setCollegeId(college.getId());
        major.setTopicGroupId(topicGroup.getId());
        ThrowUtils.throwIf(!majorService.save(major), CodeBindMessageEnums.OPERATION_ERROR, "无法添加新的专业");
        return major.getId();
    }

    /**
     * 校验专业和选题组属于同一学院后更新专业归属
     *
     * @param request 专业选题组更新请求
     * @return 是否更新成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateMajorGroup(MajorGroupUpdateRequest request) {
        ThrowUtils.throwIf(request == null || request.getMajorId() == null || request.getTopicGroupId() == null,
                CodeBindMessageEnums.PARAMS_ERROR, "专业和选题组不能为空");
        assert request != null;
        Major major = majorService.getById(request.getMajorId());
        ThrowUtils.throwIf(major == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "专业不存在");
        TopicGroup topicGroup = requireTopicGroup(request.getTopicGroupId());
        ThrowUtils.throwIf(!major.getCollegeId().equals(topicGroup.getCollegeId()),
                CodeBindMessageEnums.PARAMS_ERROR, "选题组不属于专业所在学院");
        major.setTopicGroupId(topicGroup.getId());
        ThrowUtils.throwIf(!majorService.updateById(major), CodeBindMessageEnums.OPERATION_ERROR, "无法保存专业选题组");
        return true;
    }

    /**
     * 校验学院和名称唯一性后保存选题组
     *
     * @param request 选题组创建请求
     * @return 新增选题组 id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addTopicGroup(TopicGroupAddRequest request) {
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;
        String groupName = StringUtils.trimToNull(request.getGroupName());
        ThrowUtils.throwIf(request.getCollegeId() == null || groupName == null,
                CodeBindMessageEnums.PARAMS_ERROR, "学院和选题组名称不能为空");
        requireCollege(request.getCollegeId());
        ThrowUtils.throwIf(topicGroupService.count(new QueryWrapper<TopicGroup>()
                        .eq("collegeId", request.getCollegeId()).eq("groupName", groupName)) > 0,
                CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "该学院已存在同名选题组");
        TopicGroup topicGroup = new TopicGroup();
        topicGroup.setCollegeId(request.getCollegeId());
        topicGroup.setGroupName(groupName);
        ThrowUtils.throwIf(!topicGroupService.save(topicGroup), CodeBindMessageEnums.OPERATION_ERROR, "无法添加选题组");
        return topicGroup.getId();
    }

    /**
     * 禁止跨学院移动并更新选题组名称
     *
     * @param request 选题组更新请求
     * @return 是否更新成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateTopicGroup(TopicGroupUpdateRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() == null,
                CodeBindMessageEnums.PARAMS_ERROR, "选题组 id 不能为空");
        assert request != null;
        TopicGroup topicGroup = requireTopicGroup(request.getId());
        Long collegeId = request.getCollegeId() == null ? topicGroup.getCollegeId() : request.getCollegeId();
        ThrowUtils.throwIf(!topicGroup.getCollegeId().equals(collegeId),
                CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "不允许将选题组移动到其他学院");
        String groupName = StringUtils.trimToNull(request.getGroupName());
        ThrowUtils.throwIf(groupName == null, CodeBindMessageEnums.PARAMS_ERROR, "选题组名称不能为空");
        ThrowUtils.throwIf(topicGroupService.count(new QueryWrapper<TopicGroup>()
                        .eq("collegeId", collegeId).eq("groupName", groupName).ne("id", topicGroup.getId())) > 0,
                CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "该学院已存在同名选题组");
        topicGroup.setGroupName(groupName);
        ThrowUtils.throwIf(!topicGroupService.updateById(topicGroup),
                CodeBindMessageEnums.OPERATION_ERROR, "无法更新选题组");
        return true;
    }

    /**
     * 校验专业、负责人、题目和额度均未引用后删除选题组
     *
     * @param request 选题组删除请求
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteTopicGroup(TopicGroupDeleteRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() == null,
                CodeBindMessageEnums.PARAMS_ERROR, "选题组 id 不能为空");
        assert request != null;
        Long topicGroupId = request.getId();
        requireTopicGroup(topicGroupId);
        ThrowUtils.throwIf(majorService.count(new QueryWrapper<Major>().eq("topicGroupId", topicGroupId)) > 0,
                CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "请先解除该选题组关联的专业");
        ThrowUtils.throwIf(userService.count(new QueryWrapper<User>().eq("topicGroupId", topicGroupId)) > 0,
                CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "请先解除该选题组关联的负责人");
        ThrowUtils.throwIf(topicService.count(new QueryWrapper<Topic>().eq("topicGroupId", topicGroupId)) > 0,
                CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "请先删除该选题组关联的题目");
        ThrowUtils.throwIf(!teacherGroupService.teacherAccountsForGroup(topicGroupId).isEmpty(),
                CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "请先删除该选题组的教师额度配置");
        ThrowUtils.throwIf(!topicGroupService.removeById(topicGroupId),
                CodeBindMessageEnums.OPERATION_ERROR, "无法删除选题组");
        return true;
    }

    /**
     * 校验学院不存在专业、选题组和用户引用后删除学院
     *
     * @param request 学院删除请求
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteCollege(DeleteCollegeRequest request) {
        ThrowUtils.throwIf(request == null || request.getCollegeId() == null,
                CodeBindMessageEnums.PARAMS_ERROR, "学院 id 不能为空");
        assert request != null;
        Long collegeId = request.getCollegeId();
        requireCollege(collegeId);
        ThrowUtils.throwIf(majorService.count(new QueryWrapper<Major>().eq("collegeId", collegeId)) > 0,
                CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "请先删除学院下的所有专业");
        ThrowUtils.throwIf(topicGroupService.count(new QueryWrapper<TopicGroup>().eq("collegeId", collegeId)) > 0,
                CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "请先删除学院下的所有选题组");
        ThrowUtils.throwIf(userService.count(new QueryWrapper<User>().eq("collegeId", collegeId)) > 0,
                CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "请先删除学院下的所有用户");
        ThrowUtils.throwIf(!collegeService.removeById(collegeId), CodeBindMessageEnums.OPERATION_ERROR, "无法删除学院");
        return true;
    }

    /**
     * 校验专业不存在用户引用后删除专业
     *
     * @param request 专业删除请求
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteMajor(DeleteMajorRequest request) {
        ThrowUtils.throwIf(request == null || request.getMajorId() == null,
                CodeBindMessageEnums.PARAMS_ERROR, "专业 id 不能为空");
        assert request != null;
        Long majorId = request.getMajorId();
        ThrowUtils.throwIf(majorService.getById(majorId) == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "专业不存在");
        ThrowUtils.throwIf(userService.count(new QueryWrapper<User>().eq("majorId", majorId)) > 0,
                CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "请先删除该专业关联的用户");
        ThrowUtils.throwIf(!majorService.removeById(majorId), CodeBindMessageEnums.OPERATION_ERROR, "无法删除专业");
        return true;
    }

    /// 组织读用例 ///

    /**
     * 使用 MyBatis-Plus 分页查询学院记录
     *
     * @param request 学院查询请求
     * @return 学院分页数据
     */
    @Override
    public Page<College> getCollegePage(CollegeQueryRequest request) {
        validatePage(request.getCurrent(), request.getPageSize());
        return collegeService.page(new Page<>(request.getCurrent(), request.getPageSize()),
                collegeService.getQueryWrapper(request));
    }

    /**
     * 按管理员或当前用户学院范围组装学院下拉选项
     *
     * @param request 学院查询请求
     * @return 学院下拉选项
     */
    @Override
    public List<CollegeVO> getCollegeList(CollegeQueryRequest request) {
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        User loginUser = userService.userGetCurrentLoginUser();
        QueryWrapper<College> query = new QueryWrapper<>();
        if (!userService.userIsAdmin(loginUser)) {
            ThrowUtils.throwIf(loginUser.getCollegeId() == null,
                    CodeBindMessageEnums.NO_AUTH_ERROR, "当前用户未配置所属学院");
            query.eq("id", loginUser.getCollegeId());
        }
        List<CollegeVO> result = new ArrayList<>();
        for (College college : collegeService.list(query)) {
            CollegeVO option = new CollegeVO();
            option.setValue(college.getId());
            option.setLabel(college.getCollegeName());
            result.add(option);
        }
        return result;
    }

    /**
     * 使用 MyBatis-Plus 分页查询专业记录
     *
     * @param request 专业查询请求
     * @return 专业分页数据
     */
    @Override
    public Page<Major> getMajorPage(MajorQueryRequest request) {
        validatePage(request.getCurrent(), request.getPageSize());
        return majorService.page(new Page<>(request.getCurrent(), request.getPageSize()),
                majorService.getQueryWrapper(request));
    }

    /**
     * 按学院条件查询专业并组装下拉选项
     *
     * @param request 专业查询请求
     * @return 专业下拉选项
     */
    @Override
    public List<MajorVO> getMajorList(MajorQueryRequest request) {
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        validatePage(request.getCurrent(), request.getPageSize());
        User loginUser = userService.userGetCurrentLoginUser();
        if (!userService.userIsAdmin(loginUser)) {
            request.setCollegeId(loginUser.getCollegeId());
        }
        Page<Major> page = majorService.page(new Page<>(request.getCurrent(), request.getPageSize()),
                majorService.getQueryWrapper(request));
        return page.getRecords().stream().map(major -> {
            MajorVO option = new MajorVO();
            option.setValue(major.getId());
            option.setLabel(major.getMajorName());
            return option;
        }).collect(Collectors.toList());
    }

    /**
     * 使用 MyBatis-Plus 分页查询选题组记录
     *
     * @param request 选题组查询请求
     * @return 选题组分页数据
     */
    @Override
    public Page<TopicGroup> getTopicGroupPage(TopicGroupQueryRequest request) {
        validatePage(request.getCurrent(), request.getPageSize());
        QueryWrapper<TopicGroup> query = buildTopicGroupQuery(request);
        return topicGroupService.page(new Page<>(request.getCurrent(), request.getPageSize()), query);
    }

    /**
     * 按学院和当前用户范围查询选题组选项
     *
     * @param request 选题组查询请求
     * @return 选题组选项
     */
    @Override
    public List<TopicGroupVO> getTopicGroupList(TopicGroupQueryRequest request) {
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        User loginUser = userService.userGetCurrentLoginUser();
        if (!userService.userIsAdmin(loginUser)) {
            request.setCollegeId(loginUser.getCollegeId());
        }
        return topicGroupService.list(buildTopicGroupQuery(request)).stream().map(group -> {
            TopicGroupVO option = new TopicGroupVO();
            option.setValue(group.getId());
            option.setLabel(group.getGroupName());
            option.setCollegeId(group.getCollegeId());
            return option;
        }).collect(Collectors.toList());
    }

    /// 教师组选题额度查询 ///

    /**
     * 使用当前教师账号查询各选题组额度
     *
     * @return 当前教师组选题额度
     */
    @Override
    public List<Map<String, Object>> getTeacherGroups() {
        return teacherGroupService.groups(userService.userGetCurrentLoginUser().getUserAccount());
    }

    /**
     * 将选题负责人可见教师限制到负责人所属选题组后批量查询额度
     *
     * @param request 教师组选题额度批量查询请求
     * @return 教师账号到组选题额度的映射
     */
    @Override
    public Map<String, List<Map<String, Object>>> getTeacherGroupsBatch(TeacherGroupsBatchRequest request) {
        ThrowUtils.throwIf(request == null || request.getTeacherAccounts() == null,
                CodeBindMessageEnums.PARAMS_ERROR, "教师账号列表不能为空");
        List<String> accounts = request.getTeacherAccounts();
        User loginUser = userService.userGetCurrentLoginUser();
        if (Boolean.TRUE.equals(userService.userIsTopicLeader(loginUser))) {
            ThrowUtils.throwIf(loginUser.getTopicGroupId() == null,
                    CodeBindMessageEnums.NO_AUTH_ERROR, "当前选题负责人未配置选题组");
            Set<String> allowedAccounts = new HashSet<>(
                    teacherGroupService.teacherAccountsForGroup(loginUser.getTopicGroupId()));
            accounts = accounts.stream().filter(allowedAccounts::contains).collect(Collectors.toList());
        }
        Map<String, List<Map<String, Object>>> result = teacherGroupService.groupsBatch(accounts);
        if (Boolean.TRUE.equals(userService.userIsTopicLeader(loginUser))) {
            Long topicGroupId = loginUser.getTopicGroupId();
            result.values().forEach(rows -> rows.removeIf(row ->
                    !topicGroupId.equals(((Number) row.get("topicGroupId")).longValue())));
        }
        return result;
    }

    /**
     * 校验教师、选题组和学院归属后更新 teacher_group_quota 中的最大出题数量
     *
     * @param request 教师选题组额度更新请求
     * @return 是否更新成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateTeacherGroupQuota(TeacherGroupQuotaUpdateRequest request) {
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;
        String teacherAccount = StringUtils.trimToNull(request.getTeacherAccount());
        Long topicGroupId = request.getTopicGroupId();
        Integer maxTopics = request.getMaxTopics();
        ThrowUtils.throwIf(teacherAccount == null || topicGroupId == null,
                CodeBindMessageEnums.PARAMS_ERROR, "教师账号和选题组不能为空");
        ThrowUtils.throwIf(maxTopics == null || maxTopics < 0 || maxTopics > 20,
                CodeBindMessageEnums.PARAMS_ERROR, "最大出题数量必须在 0 到 20 之间");

        User teacher = userService.getOne(new QueryWrapper<User>().eq("userAccount", teacherAccount));
        ThrowUtils.throwIf(teacher == null || !Boolean.TRUE.equals(userService.userIsTeacher(teacher)),
                CodeBindMessageEnums.NOT_FOUND_ERROR, "教师账号不存在");
        assert teacher != null;
        TopicGroup topicGroup = requireTopicGroup(topicGroupId);
        ThrowUtils.throwIf(teacher.getCollegeId() == null
                        || !teacher.getCollegeId().equals(topicGroup.getCollegeId()),
                CodeBindMessageEnums.PARAMS_ERROR, "教师与选题组不属于同一学院");
        teacherGroupService.updateQuota(teacherAccount, topicGroupId, maxTopics);
        return true;
    }

    /**
     * 查询所有有效选题组名称
     *
     * @return 选题组名称列表
     */
    @Override
    public List<String> getGroupList() {
        return teacherGroupService.allGroups();
    }

    /// 私有校验 ///

    /**
     * 根据 id 查询学院并校验存在性
     *
     * @param collegeId 学院 id
     * @return 学院实体
     */
    private College requireCollege(Long collegeId) {
        College college = collegeId == null ? null : collegeService.getById(collegeId);
        ThrowUtils.throwIf(college == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "学院不存在");
        return college;
    }

    /**
     * 根据 id 查询选题组并校验存在性
     *
     * @param topicGroupId 选题组 id
     * @return 选题组实体
     */
    private TopicGroup requireTopicGroup(Long topicGroupId) {
        TopicGroup topicGroup = topicGroupId == null ? null : topicGroupService.getById(topicGroupId);
        ThrowUtils.throwIf(topicGroup == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "选题组不存在");
        return topicGroup;
    }

    /**
     * 校验分页参数范围
     *
     * @param current 当前页
     * @param pageSize 每页数量
     */
    private void validatePage(long current, long pageSize) {
        ThrowUtils.throwIf(current < 1, CodeBindMessageEnums.PARAMS_ERROR, "页码号必须大于 0");
        ThrowUtils.throwIf(pageSize < 1 || pageSize > 100,
                CodeBindMessageEnums.PARAMS_ERROR, "页大小必须在 1 到 100 之间");
    }

    /**
     * 根据学院和名称组装选题组查询条件
     *
     * @param request 选题组查询请求
     * @return 选题组查询条件
     */
    private QueryWrapper<TopicGroup> buildTopicGroupQuery(TopicGroupQueryRequest request) {
        QueryWrapper<TopicGroup> query = new QueryWrapper<>();
        query.eq(request.getCollegeId() != null, "collegeId", request.getCollegeId());
        query.like(StringUtils.isNotBlank(request.getGroupName()), "groupName", request.getGroupName());
        query.orderByAsc("groupName");
        return query;
    }

}
