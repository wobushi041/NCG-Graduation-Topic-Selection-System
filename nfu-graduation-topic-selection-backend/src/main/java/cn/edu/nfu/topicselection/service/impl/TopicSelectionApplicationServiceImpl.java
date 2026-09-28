package cn.edu.nfu.topicselection.service.impl;

import cn.edu.nfu.topicselection.constant.TopicConstant;
import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import cn.edu.nfu.topicselection.manager.redis.RedisManager;
import cn.edu.nfu.topicselection.mapper.StudentTopicSelectionMapper;
import cn.edu.nfu.topicselection.mapper.TopicMapper;
import cn.edu.nfu.topicselection.mapper.UserMapper;
import cn.edu.nfu.topicselection.model.entity.Project;
import cn.edu.nfu.topicselection.model.entity.StudentTopicSelection;
import cn.edu.nfu.topicselection.model.entity.Topic;
import cn.edu.nfu.topicselection.model.entity.User;
import cn.edu.nfu.topicselection.model.enums.StudentTopicSelectionStatusEnum;
import cn.edu.nfu.topicselection.model.enums.TopicStatusEnum;
import cn.edu.nfu.topicselection.model.request.selection.SelectStudentRequest;
import cn.edu.nfu.topicselection.model.request.selection.SelectTopicByIdRequest;
import cn.edu.nfu.topicselection.model.request.selection.WithdrawRequest;
import cn.edu.nfu.topicselection.service.MailService;
import cn.edu.nfu.topicselection.service.ProjectService;
import cn.edu.nfu.topicselection.service.StudentTopicSelectionService;
import cn.edu.nfu.topicselection.service.SwitchService;
import cn.edu.nfu.topicselection.service.TopicSelectionApplicationService;
import cn.edu.nfu.topicselection.service.TopicService;
import cn.edu.nfu.topicselection.service.UserService;
import cn.edu.nfu.topicselection.utils.ThrowUtils;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 基于数据库悲观锁、Redis 系统开关和邮件通知实现学生选题与教师确认写用例
 *
 * @author wobushi041
 */
@Service
public class TopicSelectionApplicationServiceImpl implements TopicSelectionApplicationService {

    /**
     * 注入用户数据访问层依赖
     */
    private final UserMapper userMapper;

    /**
     * 注入课题数据访问层依赖
     */
    private final TopicMapper topicMapper;

    /**
     * 注入学生选题关联数据访问层依赖
     */
    private final StudentTopicSelectionMapper studentTopicSelectionMapper;

    /**
     * 注入用户服务依赖
     */
    private final UserService userService;

    /**
     * 注入专业服务依赖
     */
    private final ProjectService projectService;

    /**
     * 注入课题服务依赖
     */
    private final TopicService topicService;

    /**
     * 注入学生选题关联服务依赖
     */
    private final StudentTopicSelectionService studentTopicSelectionService;

    /**
     * 注入系统开关服务依赖
     */
    private final SwitchService switchService;

    /**
     * 注入 Redis 缓存管理器依赖
     */
    private final RedisManager redisManager;

    /**
     * 注入邮箱通知服务依赖
     */
    private final MailService mailService;

    /**
     * 初始化学生选题与教师确认写用例服务实现
     *
     * @param userMapper                   用户数据访问层
     * @param topicMapper                  课题数据访问层
     * @param studentTopicSelectionMapper  学生选题关联数据访问层
     * @param userService                  用户服务
     * @param projectService               专业服务
     * @param topicService                 课题服务
     * @param studentTopicSelectionService 学生选题关联服务
     * @param switchService                系统开关服务
     * @param redisManager                 Redis 缓存管理器
     * @param mailService                  邮箱通知服务
     */
    public TopicSelectionApplicationServiceImpl(UserMapper userMapper, TopicMapper topicMapper,
                                                StudentTopicSelectionMapper studentTopicSelectionMapper,
                                                UserService userService, ProjectService projectService,
                                                TopicService topicService,
                                                StudentTopicSelectionService studentTopicSelectionService,
                                                SwitchService switchService, RedisManager redisManager,
                                                MailService mailService) {
        this.userMapper = userMapper;
        this.topicMapper = topicMapper;
        this.studentTopicSelectionMapper = studentTopicSelectionMapper;
        this.userService = userService;
        this.projectService = projectService;
        this.topicService = topicService;
        this.studentTopicSelectionService = studentTopicSelectionService;
        this.switchService = switchService;
        this.redisManager = redisManager;
        this.mailService = mailService;
    }

    /// 选题写用例 ///

    /**
     * 在事务中按固定顺序加悲观锁执行预选或取消预选并同步更新题目预选人数
     *
     * @param request 预选或取消预选请求
     * @return 操作关联的题目 id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long preselectTopicById(SelectTopicByIdRequest request) {
        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        Long topicId = request.getId();
        ThrowUtils.throwIf(topicId == null, CodeBindMessageEnums.PARAMS_ERROR, "题目 id 不能为空");

        Integer status = request.getStatus();
        ThrowUtils.throwIf(status == null, CodeBindMessageEnums.PARAMS_ERROR, "操作状态不能为空");

        StudentTopicSelectionStatusEnum studentTopicSelectionStatusEnum = StudentTopicSelectionStatusEnum.getEnums(status);
        ThrowUtils.throwIf(studentTopicSelectionStatusEnum == null, CodeBindMessageEnums.PARAMS_ERROR, "不存在这种状态");
        ThrowUtils.throwIf(
                studentTopicSelectionStatusEnum != StudentTopicSelectionStatusEnum.EN_PRESELECT
                        && studentTopicSelectionStatusEnum != StudentTopicSelectionStatusEnum.UN_PRESELECT,
                CodeBindMessageEnums.PARAMS_ERROR,
                "该接口只允许预选或取消预选"
        );

        // 悲观锁加锁与预选状态更新
        User loginUser = userService.userGetCurrentLoginUser();
        User lockedStudent = userMapper.selectByIdForUpdate(loginUser.getId());
        ThrowUtils.throwIf(
                lockedStudent == null || !userService.userIsStudent(lockedStudent),
                CodeBindMessageEnums.NO_AUTH_ERROR,
                "当前账号不是有效学生账号"
        );

        Topic topic = topicMapper.selectByIdForUpdate(topicId);
        ThrowUtils.throwIf(topic == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "该题目不存在");

        List<StudentTopicSelection> selections = studentTopicSelectionMapper.selectByUserForUpdate(lockedStudent.getUserAccount());
        StudentTopicSelection targetSelection = selections.stream()
                .filter(item -> Objects.equals(item.getTopicId(), topicId))
                .findFirst()
                .orElse(null);

        if (studentTopicSelectionStatusEnum == StudentTopicSelectionStatusEnum.EN_PRESELECT) {
            validateStudentTopicGroup(lockedStudent, topic);
            boolean crossSelectionEnabled = switchService.isEnabled(TopicConstant.CROSS_TOPIC_SWITCH);
            ThrowUtils.throwIf(
                    !crossSelectionEnabled && !Objects.equals(lockedStudent.getDept(), topic.getDeptName()),
                    CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR,
                    "不允许跨系部选题, 请等待开放"
            );
            ThrowUtils.throwIf(
                    crossSelectionEnabled && !this.isStudentAllowedCrossSelect(lockedStudent.getDept(), topic.getDeptName()),
                    CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR,
                    "当前系统配置不允许预选该系部题目"
            );
            ThrowUtils.throwIf(targetSelection != null, CodeBindMessageEnums.OPERATION_ERROR, "不能重复预选该题目");
            ThrowUtils.throwIf(
                    selections.stream().anyMatch(this::isFinalSelection),
                    CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR,
                    "您已经提交了题目, 不能再预选新的题目了"
            );
            ThrowUtils.throwIf(selections.size() >= 10, CodeBindMessageEnums.OPERATION_ERROR, "最多只能预选 10 个题目");
            ThrowUtils.throwIf(topic.getSurplusQuantity() <= 0, CodeBindMessageEnums.OPERATION_ERROR, "选题余量不足, 无法选择该题目");

            targetSelection = new StudentTopicSelection();
            targetSelection.setUserAccount(lockedStudent.getUserAccount());
            targetSelection.setTopicId(topicId);
            targetSelection.setStatus(StudentTopicSelectionStatusEnum.EN_PRESELECT.getCode());
            boolean saved = studentTopicSelectionService.save(targetSelection);
            ThrowUtils.throwIf(!saved, CodeBindMessageEnums.OPERATION_ERROR, "无法保存预选，请联系系统管理员");
            topic.setSelectAmount(topic.getSelectAmount() + 1);
        } else {
            // 取消已有的预选记录并回退预选计数
            ThrowUtils.throwIf(
                    targetSelection == null || !Objects.equals(targetSelection.getStatus(), StudentTopicSelectionStatusEnum.EN_PRESELECT.getCode()),
                    CodeBindMessageEnums.NOT_FOUND_ERROR,
                    "不存在需要取消预选的题目"
            );
            boolean removed = studentTopicSelectionService.removeById(targetSelection.getId());
            ThrowUtils.throwIf(!removed, CodeBindMessageEnums.OPERATION_ERROR, "无法取消预选");
            topic.setSelectAmount(Math.max(0, topic.getSelectAmount() - 1));
        }

        boolean topicUpdated = topicService.updateById(topic);
        ThrowUtils.throwIf(!topicUpdated, CodeBindMessageEnums.OPERATION_ERROR, "无法保存预选人数");
        return topic.getId();
    }

    /**
     * 在事务中按固定顺序加悲观锁确认学生最终选题并扣减题目剩余余量
     *
     * @param request 确认提交选题请求
     * @return 选题关联记录 id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long selectTopicById(SelectTopicByIdRequest request) {
        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        Long topicId = request.getId();
        ThrowUtils.throwIf(topicId == null, CodeBindMessageEnums.PARAMS_ERROR, "题目 id 不能为空");

        Integer status = request.getStatus();
        ThrowUtils.throwIf(status == null, CodeBindMessageEnums.PARAMS_ERROR, "请添加选择操作状态");

        StudentTopicSelectionStatusEnum statusEnums = StudentTopicSelectionStatusEnum.getEnums(status);
        ThrowUtils.throwIf(
                statusEnums != StudentTopicSelectionStatusEnum.EN_SELECT,
                CodeBindMessageEnums.PARAMS_ERROR,
                "该接口只允许确认选题，退选请使用退选接口"
        );

        // 执行悲观锁确认选题
        User loginUser = userService.userGetCurrentLoginUser();
        return confirmStudentSelection(loginUser, topicId);
    }

    /**
     * 在事务中校验双选开关并加悲观锁由教师为指定学生确认本人名下课题
     *
     * @param request 教师选择学生请求
     * @return 选题关联记录 id 字符串
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String selectStudent(SelectStudentRequest request) {
        // 检查当前单选模式能否可双选
        ThrowUtils.throwIf(
                switchService.isEnabled(TopicConstant.SWITCH_SINGLE_CHOICE),
                CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR,
                "当前模式为学生选择教师模式, 无法双选"
        );

        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        String userAccount = request.getUserAccount();
        ThrowUtils.throwIf(StringUtils.isBlank(userAccount), CodeBindMessageEnums.PARAMS_ERROR, "用户账号不能为空");

        String topicName = request.getTopic();
        ThrowUtils.throwIf(StringUtils.isBlank(topicName), CodeBindMessageEnums.PARAMS_ERROR, "课题名称不能为空");

        // 查询当前教师与目标学生及课题并加锁确认
        User loginTeacher = userService.userGetCurrentLoginUser();
        User student = userService.userIsExist(userAccount);
        ThrowUtils.throwIf(
                student == null || !userService.userIsStudent(student),
                CodeBindMessageEnums.NOT_FOUND_ERROR,
                "未找到对应的学生账号"
        );

        Topic topic = topicService.getOne(new QueryWrapper<Topic>()
                .eq("topic", topicName)
                .eq("teacherAccount", loginTeacher.getUserAccount())
        );
        ThrowUtils.throwIf(topic == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "未找到当前教师名下的课题");
        return assignStudentSelection(loginTeacher, student, topic.getId());
    }

    /**
     * 在事务中加悲观锁执行教师或学生退选操作并恢复课题余量与发送通知邮件
     *
     * @param request 退选请求
     * @return 是否退选成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean withdraw(WithdrawRequest request) {
        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        Long topicId = request.getId();
        ThrowUtils.throwIf(topicId == null, CodeBindMessageEnums.PARAMS_ERROR, "id 不能为空");
        ThrowUtils.throwIf(topicId <= 0, CodeBindMessageEnums.PARAMS_ERROR, "id 必须是正整数");

        // 校验退选发起人角色与目标学生
        User loginUser = userService.userGetCurrentLoginUser();
        boolean studentOperation = userService.userIsStudent(loginUser);
        String requestedAccount = request.getUserAccount();
        if (studentOperation) {
            ThrowUtils.throwIf(
                    StringUtils.isNotBlank(requestedAccount)
                            && !Objects.equals(requestedAccount, loginUser.getUserAccount()),
                    CodeBindMessageEnums.NO_AUTH_ERROR,
                    "学生只能退选自己的题目"
            );
            requestedAccount = loginUser.getUserAccount();
        } else {
            // 教师发起退选必须显式传入学生账号
            ThrowUtils.throwIf(!userService.userIsTeacher(loginUser), CodeBindMessageEnums.NO_AUTH_ERROR, "当前角色不能退选");
            ThrowUtils.throwIf(StringUtils.isBlank(requestedAccount), CodeBindMessageEnums.PARAMS_ERROR, "教师退选时必须指定学生账号");
        }

        User selectedStudent = userService.userIsExist(requestedAccount);
        ThrowUtils.throwIf(
                selectedStudent == null || !userService.userIsStudent(selectedStudent),
                CodeBindMessageEnums.NOT_FOUND_ERROR,
                "未找到对应的学生账号"
        );
        return withdrawSelection(loginUser, selectedStudent, topicId, studentOperation);
    }

    /// 内部事务与领域校验辅助方法 ///

    /**
     * 在事务中加悲观锁确认学生最终选题并扣减题目余量
     *
     * @param loginUser 当前登录学生
     * @param topicId   题目 id
     * @return 确认成功的选题关联记录 id
     */
    public Long confirmStudentSelection(User loginUser, Long topicId) {
        User lockedStudent = userMapper.selectByIdForUpdate(loginUser.getId());
        ThrowUtils.throwIf(
                lockedStudent == null || !userService.userIsStudent(lockedStudent),
                CodeBindMessageEnums.NO_AUTH_ERROR,
                "当前账号不是有效学生账号"
        );

        Topic topic = topicMapper.selectByIdForUpdate(topicId);
        ThrowUtils.throwIf(topic == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "该题目不存在");
        validateStudentTopicGroup(lockedStudent, topic);
        ThrowUtils.throwIf(
                !Objects.equals(topic.getStatus(), TopicStatusEnum.PUBLISHED.getCode()),
                CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR,
                "该题目未发布, 还不可以选中"
        );

        Date startTime = topic.getStartTime();
        Date endTime = topic.getEndTime();
        ThrowUtils.throwIf(startTime == null || endTime == null, CodeBindMessageEnums.PARAMS_ERROR, "题目开放时间未设置完整");
        Date now = new Date();
        ThrowUtils.throwIf(
                now.before(startTime) || now.after(endTime),
                CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR,
                "当前不在选题开放范围内, 请等待管理员开放选题"
        );

        ThrowUtils.throwIf(
                !switchService.isEnabled(TopicConstant.SWITCH_SINGLE_CHOICE),
                CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR,
                "当前模式为教师选择学生模式, 无法选题"
        );
        boolean crossSelectionEnabled = switchService.isEnabled(TopicConstant.CROSS_TOPIC_SWITCH);
        ThrowUtils.throwIf(
                !crossSelectionEnabled && !Objects.equals(lockedStudent.getDept(), topic.getDeptName()),
                CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR,
                "不允许跨系部选题, 请等待开放"
        );
        ThrowUtils.throwIf(
                crossSelectionEnabled && !this.isStudentAllowedCrossSelect(lockedStudent.getDept(), topic.getDeptName()),
                CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR,
                "当前系统配置不允许确认该系部题目"
        );

        List<StudentTopicSelection> selections = studentTopicSelectionMapper.selectByUserForUpdate(lockedStudent.getUserAccount());
        ThrowUtils.throwIf(
                selections.stream().anyMatch(this::isFinalSelection),
                CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR,
                "您已经提交了题目, 不能再选新的题目了"
        );
        StudentTopicSelection selection = selections.stream()
                .filter(item -> Objects.equals(item.getTopicId(), topicId))
                .findFirst()
                .orElse(null);
        ThrowUtils.throwIf(
                selection == null || !Objects.equals(selection.getStatus(), StudentTopicSelectionStatusEnum.EN_PRESELECT.getCode()),
                CodeBindMessageEnums.NOT_FOUND_ERROR,
                "您还没有预选该题目"
        );
        ThrowUtils.throwIf(topic.getSurplusQuantity() <= 0, CodeBindMessageEnums.OPERATION_ERROR, "余量不足无法选择该题目");

        selection.setStatus(StudentTopicSelectionStatusEnum.EN_SELECT.getCode());
        boolean selectionUpdated = studentTopicSelectionService.updateById(selection);
        ThrowUtils.throwIf(!selectionUpdated, CodeBindMessageEnums.OPERATION_ERROR, "无法提交选题");

        topic.setSurplusQuantity(topic.getSurplusQuantity() - 1);
        boolean topicUpdated = topicService.updateById(topic);
        ThrowUtils.throwIf(!topicUpdated, CodeBindMessageEnums.OPERATION_ERROR, "无法更新题目余量");
        return selection.getId();
    }

    /**
     * 在事务中加悲观锁由教师为指定学生分配确认题目
     *
     * @param loginTeacher 当前登录教师
     * @param student      目标学生实体
     * @param topicId      题目 id
     * @return 选题关联记录 id 字符串
     */
    public String assignStudentSelection(User loginTeacher, User student, Long topicId) {
        User lockedStudent = userMapper.selectByIdForUpdate(student.getId());
        ThrowUtils.throwIf(
                lockedStudent == null || !userService.userIsStudent(lockedStudent),
                CodeBindMessageEnums.NOT_FOUND_ERROR,
                "未找到对应的学生账号"
        );

        Topic topic = topicMapper.selectByIdForUpdate(topicId);
        ThrowUtils.throwIf(topic == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "未找到对应的课题");
        validateStudentTopicGroup(lockedStudent, topic);
        ThrowUtils.throwIf(!isTopicOwner(loginTeacher, topic), CodeBindMessageEnums.NO_AUTH_ERROR, "只能为自己的题目选择学生");
        ThrowUtils.throwIf(
                !Objects.equals(topic.getStatus(), TopicStatusEnum.PUBLISHED.getCode()),
                CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR,
                "该课题尚未发布, 暂时无法选择学生"
        );

        List<StudentTopicSelection> selections = studentTopicSelectionMapper.selectByUserForUpdate(lockedStudent.getUserAccount());
        ThrowUtils.throwIf(
                selections.stream().anyMatch(this::isFinalSelection),
                CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR,
                "该学生已经确定最终题目"
        );
        ThrowUtils.throwIf(topic.getSurplusQuantity() <= 0, CodeBindMessageEnums.OPERATION_ERROR, "题目余量不足");

        StudentTopicSelection selection = selections.stream()
                .filter(item -> Objects.equals(item.getTopicId(), topicId))
                .findFirst()
                .orElse(null);
        if (selection == null) {
            selection = new StudentTopicSelection();
            selection.setUserAccount(lockedStudent.getUserAccount());
            selection.setTopicId(topicId);
            selection.setStatus(StudentTopicSelectionStatusEnum.EN_SELECT.getCode());
            boolean saved = studentTopicSelectionService.save(selection);
            ThrowUtils.throwIf(!saved, CodeBindMessageEnums.OPERATION_ERROR, "保存学生选题信息失败");
            topic.setSelectAmount(topic.getSelectAmount() + 1);
        } else {
            // 学生已预选该题目时直接将预选记录升级为最终确认状态
            ThrowUtils.throwIf(
                    !Objects.equals(selection.getStatus(), StudentTopicSelectionStatusEnum.EN_PRESELECT.getCode()),
                    CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR,
                    "该学生的题目关联状态不允许确认"
            );
            selection.setStatus(StudentTopicSelectionStatusEnum.EN_SELECT.getCode());
            boolean updated = studentTopicSelectionService.updateById(selection);
            ThrowUtils.throwIf(!updated, CodeBindMessageEnums.OPERATION_ERROR, "更新学生选题信息失败");
        }

        topic.setSurplusQuantity(topic.getSurplusQuantity() - 1);
        boolean topicUpdated = topicService.updateById(topic);
        ThrowUtils.throwIf(!topicUpdated, CodeBindMessageEnums.OPERATION_ERROR, "更新课题剩余数量失败");
        return String.valueOf(selection.getId());
    }

    /**
     * 在事务中加悲观锁执行退选并恢复题目计数
     *
     * @param actor            操作人用户实体
     * @param student          目标学生实体
     * @param topicId          题目 id
     * @param studentOperation 是否为学生主动退选
     * @return 是否退选成功
     */
    public Boolean withdrawSelection(User actor, User student, Long topicId, boolean studentOperation) {
        User lockedStudent = userMapper.selectByIdForUpdate(student.getId());
        ThrowUtils.throwIf(lockedStudent == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "学生账号不存在");

        Topic topic = topicMapper.selectByIdForUpdate(topicId);
        ThrowUtils.throwIf(topic == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "未找到对应的课题, 无需退选");
        if (studentOperation) {
            ThrowUtils.throwIf(
                    !Objects.equals(actor.getId(), lockedStudent.getId()),
                    CodeBindMessageEnums.NO_AUTH_ERROR,
                    "学生只能退选自己的题目"
            );
        } else {
            // 教师操作时校验课题归属权
            ThrowUtils.throwIf(!isTopicOwner(actor, topic), CodeBindMessageEnums.NO_AUTH_ERROR, "只能从自己的题目中退选学生");
        }

        StudentTopicSelection selection = studentTopicSelectionMapper.selectByUserAndTopicForUpdate(
                lockedStudent.getUserAccount(), topicId
        );
        ThrowUtils.throwIf(
                selection == null || !isFinalSelection(selection),
                CodeBindMessageEnums.NOT_FOUND_ERROR,
                "未找到该学生的最终选题记录"
        );

        if (switchService.isEnabled(TopicConstant.TOPIC_LOCK)) {
            String lockTimestampValue = redisManager.getValue(TopicConstant.TOPIC_LOCK_TIME);
            ThrowUtils.throwIf(StringUtils.isBlank(lockTimestampValue), CodeBindMessageEnums.SYSTEM_ERROR, "退选锁定时间未配置");
            long lockTimestamp = Long.parseLong(lockTimestampValue) * 1000;
            ThrowUtils.throwIf(selection.getUpdateTime() == null, CodeBindMessageEnums.SYSTEM_ERROR, "选题记录更新时间缺失");
            ThrowUtils.throwIf(
                    selection.getUpdateTime().getTime() < lockTimestamp,
                    CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR,
                    "管理员已锁定该选题，如需退选请联系管理员"
            );
        }

        restoreTopicCounters(topic, selection);
        boolean topicUpdated = topicService.updateById(topic);
        ThrowUtils.throwIf(!topicUpdated, CodeBindMessageEnums.OPERATION_ERROR, "无法恢复题目余量");

        boolean selectionRemoved = studentTopicSelectionService.removeById(selection.getId());
        ThrowUtils.throwIf(!selectionRemoved, CodeBindMessageEnums.OPERATION_ERROR, "删除学生选题记录失败");

        if (!studentOperation && StringUtils.isNotBlank(lockedStudent.getEmail())) {
            mailService.sendTopicMail(
                    lockedStudent.getEmail(),
                    "广州南方学院毕设选题管理系统",
                    "您被退选题目 [" + topic.getTopic() + "]，操作人为 " + actor.getUserName()
            );
        }
        return true;
    }

    /**
     * 校验学生所属专业是否属于课题限定的选题组
     *
     * @param student 学生实体
     * @param topic   选题实体
     */
    public void validateStudentTopicGroup(User student, Topic topic) {
        if (StringUtils.isBlank(topic.getTopicGroup())) {
            return;
        }
        ThrowUtils.throwIf(StringUtils.isBlank(student.getProject()), CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "当前学生未配置专业，无法选择分组题目");
        Project project = projectService.getOne(new QueryWrapper<Project>().eq("projectName", student.getProject()));
        ThrowUtils.throwIf(project == null || StringUtils.isBlank(project.getGroupName()), CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "当前专业未配置选题组，无法选择该题目");
        ThrowUtils.throwIf(
                !Objects.equals(StringUtils.trimToNull(project.getGroupName()), StringUtils.trimToNull(topic.getTopicGroup())),
                CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR,
                "当前专业不属于该题目适用的选题组"
        );
    }

    /**
     * 判断该系部的学生在跨选配置中是否允许跨选目标系部
     *
     * @param userDeptName 学生所属系部名称
     * @param objDeptName  目标课题所属系部名称
     * @return 是否允许跨选
     */
    private Boolean isStudentAllowedCrossSelect(String userDeptName, String objDeptName) {
        // 检查参数
        ThrowUtils.throwIf(StringUtils.isBlank(userDeptName), CodeBindMessageEnums.PARAMS_ERROR, "用户系部不能为空");
        ThrowUtils.throwIf(StringUtils.isBlank(objDeptName), CodeBindMessageEnums.PARAMS_ERROR, "目标系部不能为空");

        // 如果有存在配置就默认按照规则跨选, 不存在就直接允许跨选所有系部
        String value = redisManager.getValue(TopicConstant.DEPT_CROSS_TOPIC_CONFIG + ":" + userDeptName);
        if (value == null) {
            return true;
        }
        List<String> enableSelectDepts = JSONUtil.toList(value, String.class);
        return enableSelectDepts.contains(objDeptName);
    }

    /**
     * 判断指定用户是否为该课题的指导教师本人
     *
     * @param user  用户实体
     * @param topic 课题实体
     * @return 是否为课题归属教师
     */
    public boolean isTopicOwner(User user, Topic topic) {
        if (user == null || topic == null) {
            return false;
        }
        if (StringUtils.isNotBlank(topic.getTeacherAccount())) {
            return Objects.equals(user.getUserAccount(), topic.getTeacherAccount());
        }
        return Objects.equals(user.getUserName(), topic.getTeacherName())
                && Objects.equals(StringUtils.trimToNull(user.getDept()), StringUtils.trimToNull(topic.getDeptName()));
    }

    /**
     * 判断选题记录是否为已最终确认的选题状态
     *
     * @param selection 学生选题关联记录
     * @return 是否为最终选题
     */
    public boolean isFinalSelection(StudentTopicSelection selection) {
        return selection != null
                && Objects.equals(selection.getStatus(), StudentTopicSelectionStatusEnum.EN_SELECT.getCode());
    }

    /**
     * 在移除选题记录时恢复课题的预选人数与剩余可选余量计数
     *
     * @param topic     课题实体
     * @param selection 待移除的学生选题记录
     */
    public void restoreTopicCounters(Topic topic, StudentTopicSelection selection) {
        int selectAmount = topic.getSelectAmount() == null ? 0 : topic.getSelectAmount();
        topic.setSelectAmount(Math.max(0, selectAmount - 1));
        if (isFinalSelection(selection)) {
            int surplusQuantity = topic.getSurplusQuantity() == null ? 0 : topic.getSurplusQuantity();
            topic.setSurplusQuantity(surplusQuantity + 1);
        }
    }

}
