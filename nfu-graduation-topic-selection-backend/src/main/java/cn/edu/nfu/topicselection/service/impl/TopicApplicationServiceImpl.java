package cn.edu.nfu.topicselection.service.impl;

import cn.edu.nfu.topicselection.constant.TopicConstant;
import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import cn.edu.nfu.topicselection.manager.ai.AIManager;
import cn.edu.nfu.topicselection.manager.ai.AIResult;
import cn.edu.nfu.topicselection.manager.redis.RedisManager;
import cn.edu.nfu.topicselection.mapper.TopicMapper;
import cn.edu.nfu.topicselection.mapper.UserMapper;
import cn.edu.nfu.topicselection.model.entity.StudentTopicSelection;
import cn.edu.nfu.topicselection.model.entity.Topic;
import cn.edu.nfu.topicselection.model.entity.TopicGroup;
import cn.edu.nfu.topicselection.model.entity.User;
import cn.edu.nfu.topicselection.model.enums.StudentTopicSelectionStatusEnum;
import cn.edu.nfu.topicselection.model.enums.TopicStatusEnum;
import cn.edu.nfu.topicselection.model.enums.UserRoleEnum;
import cn.edu.nfu.topicselection.model.request.topic.AddTopicRequest;
import cn.edu.nfu.topicselection.model.request.topic.CheckTopicRequest;
import cn.edu.nfu.topicselection.model.request.topic.DeleteTopicRequest;
import cn.edu.nfu.topicselection.model.request.topic.GetTeacherTopicAmountRequest;
import cn.edu.nfu.topicselection.model.request.topic.GetTopicReviewLevelRequest;
import cn.edu.nfu.topicselection.model.request.topic.SetTeacherTopicAmountRequest;
import cn.edu.nfu.topicselection.model.request.topic.SetTimeRequest;
import cn.edu.nfu.topicselection.model.request.topic.UnSetTimeRequest;
import cn.edu.nfu.topicselection.model.request.topic.UpdateTopicRequest;
import cn.edu.nfu.topicselection.service.MailService;
import cn.edu.nfu.topicselection.service.StudentTopicSelectionService;
import cn.edu.nfu.topicselection.service.TeacherGroupService;
import cn.edu.nfu.topicselection.service.TopicApplicationService;
import cn.edu.nfu.topicselection.service.TopicGroupService;
import cn.edu.nfu.topicselection.service.TopicService;
import cn.edu.nfu.topicselection.service.UserService;
import cn.edu.nfu.topicselection.utils.ThrowUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 基于数据库悲观锁、状态机校验、退回邮件通知与 Redis 分布式限流实现课题全生命周期管理服务
 *
 * @author wobushi041
 */
@Service
@Slf4j
public class TopicApplicationServiceImpl implements TopicApplicationService {

    /**
     * AI 查重审核限流 Redis 键前缀
     */
    private static final String AI_REVIEW_RATE_LIMIT_PREFIX = "ai-review-rate:";

    /**
     * 注入用户数据访问层依赖
     */
    private final UserMapper userMapper;

    /**
     * 注入课题数据访问层依赖
     */
    private final TopicMapper topicMapper;

    /**
     * 注入用户服务依赖
     */
    private final UserService userService;

    /**
     * 注入课题服务依赖
     */
    private final TopicService topicService;

    /**
     * 注入学生选题关联服务依赖
     */
    private final StudentTopicSelectionService studentTopicSelectionService;

    /**
     * 注入教师选题组服务依赖
     */
    private final TeacherGroupService teacherGroupService;

    /**
     * 注入选题组服务依赖
     */
    private final TopicGroupService topicGroupService;

    /**
     * 注入邮箱通知服务依赖
     */
    private final MailService mailService;

    /**
     * 注入 Redis 缓存管理器依赖
     */
    private final RedisManager redisManager;

    /**
     * 注入 AI 查重管理器依赖
     */
    private final AIManager aiManager;

    /**
     * 初始化课题全生命周期应用服务实现
     *
     * @param userMapper                   用户数据访问层
     * @param topicMapper                  课题数据访问层
     * @param userService                  用户服务
     * @param topicService                 课题服务
     * @param studentTopicSelectionService 学生选题关联服务
     * @param teacherGroupService          教师选题组服务
     * @param topicGroupService            选题组服务
     * @param mailService                  邮箱通知服务
     * @param redisManager                 Redis 缓存管理器
     * @param aiManager                    AI 查重管理器
     */
    public TopicApplicationServiceImpl(UserMapper userMapper, TopicMapper topicMapper,
                                       UserService userService, TopicService topicService,
                                       StudentTopicSelectionService studentTopicSelectionService,
                                       TeacherGroupService teacherGroupService,
                                       TopicGroupService topicGroupService, MailService mailService,
                                       RedisManager redisManager, AIManager aiManager) {
        this.userMapper = userMapper;
        this.topicMapper = topicMapper;
        this.userService = userService;
        this.topicService = topicService;
        this.studentTopicSelectionService = studentTopicSelectionService;
        this.teacherGroupService = teacherGroupService;
        this.topicGroupService = topicGroupService;
        this.mailService = mailService;
        this.redisManager = redisManager;
        this.aiManager = aiManager;
    }

    /// 课题维护与配额写读用例 ///

    /**
     * 校验题目参数与唯一性并在事务中通过悲观锁和组选题额度校验保存新题目
     *
     * @param request 添加题目请求
     * @return 新添加的选题 id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addTopic(AddTopicRequest request) {
        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        String requestedTopicTitle = request.getTopic();
        ThrowUtils.throwIf(StringUtils.isBlank(requestedTopicTitle), CodeBindMessageEnums.PARAMS_ERROR, "题目标题不能为空");
        final String topicTitle = requestedTopicTitle.trim();
        ThrowUtils.throwIf(topicTitle.length() > 255, CodeBindMessageEnums.PARAMS_ERROR, "题目标题不能超过 255 个字符");

        String requestedTopicType = request.getType();
        ThrowUtils.throwIf(StringUtils.isBlank(requestedTopicType), CodeBindMessageEnums.PARAMS_ERROR, "题目类型不能为空");
        final String topicType = requestedTopicType.trim();
        ThrowUtils.throwIf(topicType.length() > 255, CodeBindMessageEnums.PARAMS_ERROR, "题目类型不能超过 255 个字符");

        final String topicContent = request.getDescription();
        ThrowUtils.throwIf(StringUtils.isBlank(topicContent) || topicContent.trim().length() < 5, CodeBindMessageEnums.PARAMS_ERROR, "题目描述不能为空, 并且不能少于 5 个字符");

        final String topicRequirement = request.getRequirement();
        ThrowUtils.throwIf(StringUtils.isBlank(topicRequirement), CodeBindMessageEnums.PARAMS_ERROR, "题目要求不能为空");

        final int topicSurplusQuantity = request.getSurplusQuantity() == null ? 1 : request.getSurplusQuantity();
        ThrowUtils.throwIf(topicSurplusQuantity < 1 || topicSurplusQuantity > 100, CodeBindMessageEnums.PARAMS_ERROR, "题目人数必须在 1 到 100 之间");

        Topic oldTopic = topicService.getOne(new QueryWrapper<Topic>().eq("topic", topicTitle));
        ThrowUtils.throwIf(oldTopic != null, CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "该选题已存在, 请不要重复添加");

        // 获取当前用户并执行悲观锁保存
        User user = userService.userGetCurrentLoginUser();
        User loginUser = userMapper.selectByIdForUpdate(user.getId());
        ThrowUtils.throwIf(loginUser == null || !userService.userIsTeacher(loginUser), CodeBindMessageEnums.NO_AUTH_ERROR, "当前教师账号不存在");
        Long teacherCollegeId = loginUser.getCollegeId();
        ThrowUtils.throwIf(teacherCollegeId == null, CodeBindMessageEnums.PARAMS_ERROR, "当前教师账号未配置所属学院");
        requireTeacherTopicGroup(request.getTopicGroupId(), teacherCollegeId);
        teacherGroupService.validate(loginUser.getUserAccount(), request.getTopicGroupId(), null);

        Topic topic = new Topic();
        BeanUtils.copyProperties(request, topic);
        topic.setTopic(topicTitle);
        topic.setType(topicType);
        topic.setDescription(topicContent.trim());
        topic.setRequirement(topicRequirement.trim());
        topic.setTeacherName(loginUser.getUserName());
        topic.setTeacherAccount(loginUser.getUserAccount());
        topic.setTopicGroupId(request.getTopicGroupId());
        topic.setSurplusQuantity(topicSurplusQuantity);
        boolean result = topicService.save(topic);
        ThrowUtils.throwIf(!result, CodeBindMessageEnums.OPERATION_ERROR, "无法添加新的选题");
        return topic.getId();
    }

    /**
     * 在事务中按教师到题目的固定顺序加悲观锁并删除题目及关联选题记录
     *
     * @param request 删除题目请求
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteTopic(DeleteTopicRequest request) {
        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        Long id = request.getId();
        ThrowUtils.throwIf(id == null, CodeBindMessageEnums.PARAMS_ERROR, "id 不能为空");
        assert id != null;
        ThrowUtils.throwIf(id <= 0, CodeBindMessageEnums.PARAMS_ERROR, "id 必须是正整数");

        User loginUser = userService.userGetCurrentLoginUser();
        User lockedTeacher = userMapper.selectByIdForUpdate(loginUser.getId());
        ThrowUtils.throwIf(lockedTeacher == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "当前教师不存在");

        Topic topic = topicMapper.selectByIdForUpdate(id);
        ThrowUtils.throwIf(topic == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "对应的选题不存在");
        ThrowUtils.throwIf(!isTopicOwner(lockedTeacher, topic), CodeBindMessageEnums.NO_AUTH_ERROR, "只能删除自己发布的题目");

        boolean topicRemoveResult = topicService.removeById(id);
        ThrowUtils.throwIf(!topicRemoveResult, CodeBindMessageEnums.OPERATION_ERROR, "无法删除题目");
        studentTopicSelectionService.remove(new QueryWrapper<StudentTopicSelection>().eq("topicId", id));
        return true;
    }

    /**
     * 校验教师身份并只读返回其当前配置的剩余题目上限
     *
     * @param request 获取教师题目上限请求
     * @return 教师剩余出题上限数量
     */
    @Override
    public Integer getTeacherTopicAmount(GetTeacherTopicAmountRequest request) {
        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        Long teacherId = request.getTeacherId();
        ThrowUtils.throwIf(teacherId == null || teacherId <= 0, CodeBindMessageEnums.PARAMS_ERROR, "教师标识不合法");

        // 获取教师信息
        User teacher = userService.getById(teacherId);
        ThrowUtils.throwIf(teacher == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "教师不存在");
        assert teacher != null;
        ThrowUtils.throwIf(!teacher.getUserRole().equals(UserRoleEnum.TEACHER.getCode()), CodeBindMessageEnums.PARAMS_ERROR, "该用户不是教师");

        // 返回教师题目上限
        return teacher.getTopicAmount();
    }

    /**
     * 校验教师当前已出题目数量并通过教师标识同步锁与事务更新其出题上限
     *
     * @param request 设置教师题目上限请求
     * @return 是否设置成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean setTeacherTopicAmount(SetTeacherTopicAmountRequest request) {
        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        Long teacherId = request.getTeacherId();
        ThrowUtils.throwIf(teacherId == null || teacherId <= 0, CodeBindMessageEnums.PARAMS_ERROR, "教师标识不合法");
        assert teacherId != null;

        Integer topicAmount = request.getTopicAmount();
        ThrowUtils.throwIf(topicAmount == null || topicAmount < 0 || topicAmount > 20, CodeBindMessageEnums.PARAMS_ERROR, "题目上限数量必须在 0-20 之间");
        assert topicAmount != null;

        // 获取教师信息
        User teacher = userService.getById(teacherId);
        ThrowUtils.throwIf(teacher == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "教师不存在");
        assert teacher != null;
        ThrowUtils.throwIf(!teacher.getUserRole().equals(UserRoleEnum.TEACHER.getCode()), CodeBindMessageEnums.PARAMS_ERROR, "该用户不是教师");

        // 检查教师目前的题目数量, 如果目前的题目数量已经到上限, 则不允许改小
        long currentTopicCount = topicService.count(new QueryWrapper<Topic>().eq("teacherAccount", teacher.getUserAccount()));
        ThrowUtils.throwIf(topicAmount < currentTopicCount, CodeBindMessageEnums.PARAMS_ERROR, "不能将题目上限设置为小于当前已出题目数量(" + currentTopicCount + ")");

        // 更新教师题目上限
        synchronized (String.valueOf(teacherId).intern()) {
            teacher.setTopicAmount(topicAmount);
            boolean result = userService.updateById(teacher);
            ThrowUtils.throwIf(!result, CodeBindMessageEnums.OPERATION_ERROR, "更新教师题目上限失败");
            return true;
        }
    }

    /// 课题审核、发布与 AI 查重用例 ///

    /**
     * 在事务中加悲观锁锁定课题，依据状态机校验流转权限，更新审核状态并在打回时发送理由邮件
     *
     * @param request 题目审核请求
     * @return 是否审核处理成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean checkTopic(CheckTopicRequest request) {
        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        Long id = request.getId();
        ThrowUtils.throwIf(id == null, CodeBindMessageEnums.PARAMS_ERROR, "选题 id 不能为空");
        assert id != null;
        ThrowUtils.throwIf(id <= 0, CodeBindMessageEnums.PARAMS_ERROR, "选题 id 必须是正整数");

        Integer status = request.getStatus();
        ThrowUtils.throwIf(status == null, CodeBindMessageEnums.PARAMS_ERROR, "选题状态不能为空");
        assert status != null;
        TopicStatusEnum statusEnum = TopicStatusEnum.getEnums(status);
        ThrowUtils.throwIf(statusEnum == null, CodeBindMessageEnums.PARAMS_ERROR, "未知的选题状态");

        String reason = request.getReason();
        ThrowUtils.throwIf(reason != null && reason.length() > TopicConstant.MAX_REASON_SIZE, CodeBindMessageEnums.PARAMS_ERROR, "理由过长, 不能超过 1024 符");

        User loginUser = userService.userGetCurrentLoginUser();
        Topic topic = topicMapper.selectByIdForUpdate(id);
        ThrowUtils.throwIf(topic == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "对应的选题不存在, 无需进行审核");
        ThrowUtils.throwIf(
                !isAllowedTopicStatusTransition(loginUser, topic, statusEnum),
                CodeBindMessageEnums.NO_AUTH_ERROR,
                "无权执行该题目状态变更"
        );

        boolean rejected = statusEnum == TopicStatusEnum.REJECTED;
        ThrowUtils.throwIf(
                rejected && StringUtils.isBlank(reason),
                CodeBindMessageEnums.PARAMS_ERROR,
                "打回题目时必须填写理由"
        );

        topic.setStatus(statusEnum.getCode());
        topic.setReason(rejected ? reason : "");
        boolean result = topicService.updateById(topic);
        ThrowUtils.throwIf(!result, CodeBindMessageEnums.OPERATION_ERROR, "更新题目状态失败");

        if (rejected) {
            User teacher = userService.getOne(new QueryWrapper<User>()
                    .eq("userAccount", topic.getTeacherAccount())
                    .eq("userRole", UserRoleEnum.TEACHER.getCode())
            );
            if (teacher != null && StringUtils.isNotBlank(teacher.getEmail())) {
                mailService.sendReasonMail(teacher.getEmail(), "广州南方学院毕设选题管理系统", topic.getReason());
            }
        }
        return true;
    }

    /**
     * 校验时间范围与题目列表并在事务中逐条加悲观锁设置开放时间、更新状态为已发布
     *
     * @param request 设置选题开放时间请求
     * @return 操作结果提示信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String setTimeById(SetTimeRequest request) {
        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        List<Long> topicIds = validateTopicIds(request.getTopicIds());

        Date startTime = request.getStartTime();
        ThrowUtils.throwIf(startTime == null, CodeBindMessageEnums.PARAMS_ERROR, "请选择开始时间");
        assert startTime != null;

        Date endTime = request.getEndTime();
        ThrowUtils.throwIf(endTime == null, CodeBindMessageEnums.PARAMS_ERROR, "请选择结束时间");
        assert endTime != null;

        // 时间范围检查
        ThrowUtils.throwIf(startTime.after(endTime), CodeBindMessageEnums.PARAMS_ERROR, "开始时间不能晚于结束时间");

        // 并且时间范围均不能早于当前时间
        ThrowUtils.throwIf(startTime.before(new Date()), CodeBindMessageEnums.PARAMS_ERROR, "开始时间不能早于当前时间");
        ThrowUtils.throwIf(endTime.before(new Date()), CodeBindMessageEnums.PARAMS_ERROR, "结束时间不能早于当前时间");

        // 遍历选题列表开始设置开始时间和结束时间
        for (Long topicId : topicIds) {
            Topic topic = topicMapper.selectByIdForUpdate(topicId);
            ThrowUtils.throwIf(topic == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "选中的题目不存在");
            ThrowUtils.throwIf(
                    !Objects.equals(topic.getStatus(), TopicStatusEnum.NOT_PUBLISHED.getCode())
                            && !Objects.equals(topic.getStatus(), TopicStatusEnum.PUBLISHED.getCode()),
                    CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR,
                    "只有审核通过的题目可以设置开放时间"
            );
            boolean result = topicService.update(new UpdateWrapper<Topic>()
                    .eq("id", topicId)
                    .set("status", TopicStatusEnum.PUBLISHED.getCode())
                    .set("startTime", startTime)
                    .set("endTime", endTime));
            ThrowUtils.throwIf(!result, CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "无法开放该选题，请联系系统管理员");
        }
        return "成功开放题目!";
    }

    /**
     * 在事务中逐条加悲观锁检查有效学生选题占用情况，对无占用的题目取消发布并清空时间窗口
     *
     * @param request 取消设置选题开放时间请求
     * @return 操作结果提示信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String unsetTimeById(UnSetTimeRequest request) {
        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        List<Long> topicIds = validateTopicIds(request.getTopicIds());

        // 遍历选题列表开始取消开放
        String message = "";
        for (Long topicId : topicIds) {
            Topic topic = topicMapper.selectByIdForUpdate(topicId);
            ThrowUtils.throwIf(topic == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "选中的题目不存在");
            long activeSelectionCount = studentTopicSelectionService.count(
                    new QueryWrapper<StudentTopicSelection>()
                            .eq("topicId", topicId)
                            .in("status", StudentTopicSelectionStatusEnum.EN_PRESELECT.getCode(), StudentTopicSelectionStatusEnum.EN_SELECT.getCode())
            );
            if (activeSelectionCount == 0) {
                boolean result = topicService.update(new UpdateWrapper<Topic>()
                        .eq("id", topicId)
                        .set("status", TopicStatusEnum.NOT_PUBLISHED.getCode())
                        .set("startTime", null)
                        .set("endTime", null));
                ThrowUtils.throwIf(!result, CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "无法取消开放该选题，请联系系统管理员");
            } else {
                log.debug("教师 {} 出的题目 {} - {} 已经被学生选择, 不允许取消发布, 本次跳过取消发布", topic.getTeacherName(), topic.getId(), topic.getTopic());
                message = " " + message + topic.getTeacherName() + topic.getId() + topic.getTopic();
            }
        }
        return "成功取消发布!" + (StringUtils.isNotBlank(message) ? message : "");
    }

    /**
     * 在事务中按教师到题目的顺序加悲观锁，校验未发布状态与选题组后更新题目内容并重置为待审核状态
     *
     * @param request 修改题目请求
     * @return 更新结果提示信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateTopic(UpdateTopicRequest request) {
        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        String topicName = request.getTopicName();
        ThrowUtils.throwIf(StringUtils.isBlank(topicName), CodeBindMessageEnums.PARAMS_ERROR, "需要修改题目时必须传该题目的标题");

        String type = request.getType();
        ThrowUtils.throwIf(StringUtils.isBlank(type), CodeBindMessageEnums.PARAMS_ERROR, "题目类型不能为空");

        String description = request.getDescription();
        ThrowUtils.throwIf(StringUtils.isBlank(description), CodeBindMessageEnums.PARAMS_ERROR, "题目描述不能为空");

        String requirement = request.getRequirement();
        ThrowUtils.throwIf(StringUtils.isBlank(requirement), CodeBindMessageEnums.PARAMS_ERROR, "题目要求不能为空");

        Integer requestedSurplusQuantity = request.getSurplusQuantity();
        ThrowUtils.throwIf(requestedSurplusQuantity != null && (requestedSurplusQuantity < 1 || requestedSurplusQuantity > 100),
                CodeBindMessageEnums.PARAMS_ERROR, "题目人数必须在 1 到 100 之间");

        // 获取当前登陆的教师
        User loginUser = userService.userGetCurrentLoginUser();

        Topic ownedTopic = topicService.getOne(
                new QueryWrapper<Topic>()
                        .eq("topic", topicName)
                        .eq("teacherAccount", loginUser.getUserAccount())
        );
        ThrowUtils.throwIf(ownedTopic == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "未找到当前教师名下的题目");

        userMapper.selectByIdForUpdate(loginUser.getId());
        Topic topic = topicMapper.selectByIdForUpdate(ownedTopic.getId());
        ThrowUtils.throwIf(topic == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "题目不存在");
        ThrowUtils.throwIf(!isTopicOwner(loginUser, topic), CodeBindMessageEnums.NO_AUTH_ERROR, "只能修改自己的题目");
        ThrowUtils.throwIf(
                Objects.equals(topic.getStatus(), TopicStatusEnum.PUBLISHED.getCode()),
                CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR,
                "已发布的题目不允许修改"
        );

        requireTeacherTopicGroup(request.getTopicGroupId(), loginUser.getCollegeId());
        teacherGroupService.validate(loginUser.getUserAccount(), request.getTopicGroupId(), topic.getId());
        topic.setType(type);
        topic.setDescription(description);
        topic.setRequirement(requirement);
        topic.setTopicGroupId(request.getTopicGroupId());
        if (requestedSurplusQuantity != null) {
            topic.setSurplusQuantity(requestedSurplusQuantity);
        }
        topic.setStatus(TopicStatusEnum.PENDING_REVIEW.getCode());
        topic.setReason("");
        boolean result = topicService.updateById(topic);
        ThrowUtils.throwIf(!result, CodeBindMessageEnums.SYSTEM_ERROR, "更新失败");
        return "更新成功";
    }

    /**
     * 通过 Redis 每日 30 次滑动计数限流保护高成本 AI 资源并调用大模型执行查重评估
     *
     * @param request 获取题目审核等级请求
     * @return AI 查重检测结果
     */
    @Override
    public AIResult getTopicReviewLevel(GetTopicReviewLevelRequest request) {
        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        String topicTitle = StringUtils.trim(request.getTopic());
        ThrowUtils.throwIf(StringUtils.isBlank(topicTitle), CodeBindMessageEnums.PARAMS_ERROR, "题目标题不能为空");
        ThrowUtils.throwIf(topicTitle.length() > 255, CodeBindMessageEnums.PARAMS_ERROR, "题目标题不能超过 255 个字符");

        String topicContent = StringUtils.trim(request.getDescription());
        ThrowUtils.throwIf(StringUtils.isBlank(topicContent) || topicContent.length() < 5, CodeBindMessageEnums.PARAMS_ERROR, "题目描述不能为空, 并且不能少于 5 个字符");
        ThrowUtils.throwIf(topicContent.length() > 5000, CodeBindMessageEnums.PARAMS_ERROR, "题目描述不能超过 5000 个字符");

        // 获取当前登陆用户的 id 并且转化为 UUID
        User loginUser = userService.userGetCurrentLoginUser();
        Long id = loginUser.getId();
        boolean acquired = redisManager.tryAcquire(
                AI_REVIEW_RATE_LIMIT_PREFIX + id,
                30,
                24 * 60 * 60
        );
        ThrowUtils.throwIf(!acquired, CodeBindMessageEnums.FLOW_RULES, "AI 检测每天最多使用 30 次，请稍后再试");
        String factor = "nfu-topic-selection-server";
        String raw = id + "-" + factor;
        String userId = UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8)).toString();

        String prompt = "题目标题：" + topicTitle + "\n题目描述：" + topicContent
                + "\n请判断题库中是否有相似题目，并给出约定格式的 JSON 响应。";
        AIResult aiResult = aiManager.sendAi(userId, prompt);
        ThrowUtils.throwIf(aiResult == null, CodeBindMessageEnums.OPERATION_ERROR, "AI 服务未返回有效的检测结果");
        return aiResult;
    }

    /// 领域辅助方法 ///

    /**
     * 校验当前操作人是否有权将题目流转至目标审核状态
     *
     * @param actor        操作人用户实体
     * @param topic        选题实体
     * @param targetStatus 目标选题状态枚举
     * @return 是否允许执行该状态流转
     */
    public boolean isAllowedTopicStatusTransition(User actor, Topic topic, TopicStatusEnum targetStatus) {
        if (actor == null || topic == null || targetStatus == null) {
            return false;
        }
        if (Objects.equals(actor.getUserRole(), UserRoleEnum.TOPIC_LEADER.getCode())) {
            return actor.getTopicGroupId() != null
                    && Objects.equals(actor.getTopicGroupId(), topic.getTopicGroupId())
                    && Objects.equals(topic.getStatus(), TopicStatusEnum.PENDING_REVIEW.getCode())
                    && (targetStatus == TopicStatusEnum.NOT_PUBLISHED || targetStatus == TopicStatusEnum.REJECTED);
        }
        return isTopicOwner(actor, topic)
                && Objects.equals(topic.getStatus(), TopicStatusEnum.REJECTED.getCode())
                && targetStatus == TopicStatusEnum.PENDING_REVIEW;
    }

    /**
     * 判断指定教师是否为该题目的出题人
     *
     * @param teacher 教师用户实体
     * @param topic   选题实体
     * @return 是否为题目所属教师
     */
    boolean isTopicOwner(User teacher, Topic topic) {
        return teacher != null
                && topic != null
                && Objects.equals(teacher.getUserRole(), UserRoleEnum.TEACHER.getCode())
                && StringUtils.isNotBlank(topic.getTeacherAccount())
                && Objects.equals(teacher.getUserAccount(), topic.getTeacherAccount());
    }

    /**
     * 校验并获取选题负责人直接绑定的选题组 id
     *
     * @param user 用户实体
     * @return 负责人选题组 id
     */
    Long requireUserGroup(User user) {
        ThrowUtils.throwIf(user == null || user.getTopicGroupId() == null,
                CodeBindMessageEnums.NO_AUTH_ERROR, "当前选题负责人未配置选题组");
        return user.getTopicGroupId();
    }

    /**
     * 校验选题组存在并属于教师所在学院
     *
     * @param topicGroupId 选题组 id
     * @param collegeId    教师所属学院 id
     */
    private void requireTeacherTopicGroup(Long topicGroupId, Long collegeId) {
        ThrowUtils.throwIf(topicGroupId == null, CodeBindMessageEnums.PARAMS_ERROR, "请选择选题组");
        TopicGroup topicGroup = topicGroupService.getById(topicGroupId);
        ThrowUtils.throwIf(topicGroup == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "选题组不存在");
        ThrowUtils.throwIf(collegeId == null || !collegeId.equals(topicGroup.getCollegeId()),
                CodeBindMessageEnums.NO_AUTH_ERROR, "不能为其他学院的选题组发布题目");
    }

    /**
     * 校验并去重题目 id 列表
     *
     * @param topicIds 原始题目 id 列表
     * @return 去重后的有效题目 id 列表
     */
    static List<Long> validateTopicIds(List<Long> topicIds) {
        ThrowUtils.throwIf(topicIds == null || topicIds.isEmpty(), CodeBindMessageEnums.PARAMS_ERROR, "请先选择题目");
        ThrowUtils.throwIf(topicIds.size() > 100, CodeBindMessageEnums.PARAMS_ERROR, "一次最多处理 100 个题目");
        for (Long topicId : topicIds) {
            ThrowUtils.throwIf(topicId == null || topicId <= 0, CodeBindMessageEnums.PARAMS_ERROR, "题目标识必须是正整数");
        }
        return topicIds.stream().distinct().collect(Collectors.toList());
    }

}
