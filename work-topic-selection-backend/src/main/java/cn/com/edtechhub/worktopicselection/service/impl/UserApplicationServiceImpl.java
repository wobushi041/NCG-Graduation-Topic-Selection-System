package cn.com.edtechhub.worktopicselection.service.impl;

import cn.com.edtechhub.worktopicselection.constant.UserConstant;
import cn.com.edtechhub.worktopicselection.exception.CodeBindMessageEnums;
import cn.com.edtechhub.worktopicselection.mapper.StudentTopicSelectionMapper;
import cn.com.edtechhub.worktopicselection.mapper.TopicMapper;
import cn.com.edtechhub.worktopicselection.mapper.UserMapper;
import cn.com.edtechhub.worktopicselection.model.entity.Project;
import cn.com.edtechhub.worktopicselection.model.entity.StudentTopicSelection;
import cn.com.edtechhub.worktopicselection.model.entity.Topic;
import cn.com.edtechhub.worktopicselection.model.entity.User;
import cn.com.edtechhub.worktopicselection.model.enums.StudentTopicSelectionStatusEnum;
import cn.com.edtechhub.worktopicselection.model.enums.UserRoleEnum;
import cn.com.edtechhub.worktopicselection.model.request.user.DeleteRequest;
import cn.com.edtechhub.worktopicselection.model.request.user.TeacherQueryRequest;
import cn.com.edtechhub.worktopicselection.model.request.user.UserAddRequest;
import cn.com.edtechhub.worktopicselection.model.request.user.UserQueryRequest;
import cn.com.edtechhub.worktopicselection.model.request.user.UserUpdateRequest;
import cn.com.edtechhub.worktopicselection.model.vo.LoginUserVO;
import cn.com.edtechhub.worktopicselection.model.vo.TeacherVO;
import cn.com.edtechhub.worktopicselection.model.vo.UserVO;
import cn.com.edtechhub.worktopicselection.response.BaseResponse;
import cn.com.edtechhub.worktopicselection.service.PasswordService;
import cn.com.edtechhub.worktopicselection.service.ProjectService;
import cn.com.edtechhub.worktopicselection.service.StudentTopicSelectionService;
import cn.com.edtechhub.worktopicselection.service.TopicService;
import cn.com.edtechhub.worktopicselection.service.UserApplicationService;
import cn.com.edtechhub.worktopicselection.service.UserService;
import cn.com.edtechhub.worktopicselection.utils.ThrowUtils;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 基于方法级写事务、悲观行锁余量恢复与 Sa-Token 会话管理实现用户应用服务
 *
 * @author wobushi041
 */
@Service
@Slf4j
public class UserApplicationServiceImpl implements UserApplicationService {

    /**
     * 注入用户服务依赖
     */
    private final UserService userService;

    /**
     * 注入密码服务依赖
     */
    private final PasswordService passwordService;

    /**
     * 注入用户持久层依赖
     */
    private final UserMapper userMapper;

    /**
     * 注入专业服务依赖
     */
    private final ProjectService projectService;

    /**
     * 注入课题服务依赖
     */
    private final TopicService topicService;

    /**
     * 注入课题持久层依赖
     */
    private final TopicMapper topicMapper;

    /**
     * 注入学生选题关联服务依赖
     */
    private final StudentTopicSelectionService studentTopicSelectionService;

    /**
     * 注入学生选题关联持久层依赖
     */
    private final StudentTopicSelectionMapper studentTopicSelectionMapper;

    /**
     * 初始化用户应用服务实现
     *
     * @param userService                  用户服务
     * @param passwordService              密码服务
     * @param userMapper                   用户持久层
     * @param projectService               专业服务
     * @param topicService                 课题服务
     * @param topicMapper                  课题持久层
     * @param studentTopicSelectionService 学生选题关联服务
     * @param studentTopicSelectionMapper  学生选题关联持久层
     */
    public UserApplicationServiceImpl(UserService userService, PasswordService passwordService,
                                      UserMapper userMapper, ProjectService projectService,
                                      TopicService topicService, TopicMapper topicMapper,
                                      StudentTopicSelectionService studentTopicSelectionService,
                                      StudentTopicSelectionMapper studentTopicSelectionMapper) {
        this.userService = userService;
        this.passwordService = passwordService;
        this.userMapper = userMapper;
        this.projectService = projectService;
        this.topicService = topicService;
        this.topicMapper = topicMapper;
        this.studentTopicSelectionService = studentTopicSelectionService;
        this.studentTopicSelectionMapper = studentTopicSelectionMapper;
    }

    /// 用户管理写用例 ///

    /**
     * 校验账号、姓名、角色与系部专业归属关系，生成 BCrypt 加密临时密码并在事务中落库新用户
     *
     * @param request 创建用户请求
     * @return 包含新用户 id 与一次性临时密码提示信息的统一响应
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Long> addUser(UserAddRequest request) {
        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        String userAccount = request.getUserAccount();
        ThrowUtils.throwIf(StringUtils.isBlank(userAccount), CodeBindMessageEnums.PARAMS_ERROR, "缺少用户的学号/工号");
        final String normalizedUserAccount = userAccount.trim();
        ThrowUtils.throwIf(normalizedUserAccount.length() > UserConstant.MAX_USER_ACCOUNT_LENGTH, CodeBindMessageEnums.PARAMS_ERROR, "用户账号不能超过 128 个字符");

        String userName = request.getUserName();
        ThrowUtils.throwIf(StringUtils.isBlank(userName), CodeBindMessageEnums.PARAMS_ERROR, "缺少用户名字");

        User oldUser = userService.userIsExist(normalizedUserAccount, userName);
        ThrowUtils.throwIf(oldUser != null, CodeBindMessageEnums.PARAMS_ERROR, "该学号/工号对应的用户已经存在, 请不要重复添加, 如果需要更改用户信息可以先删除用户再重新添加");

        Integer userRole = request.getUserRole();
        ThrowUtils.throwIf(userRole == null, CodeBindMessageEnums.PARAMS_ERROR, "缺少用户角色");
        assert userRole != null;

        UserRoleEnum userRoleEnum = UserRoleEnum.getEnums(userRole);
        ThrowUtils.throwIf(userRoleEnum == null, CodeBindMessageEnums.PARAMS_ERROR, "本系统不存在该用户角色");

        // 除了管理员帐号都需要系部和专业信息来注册帐号
        String userDeptName = request.getDeptName();
        if (!Objects.equals(userRoleEnum, UserRoleEnum.ADMIN)) {
            ThrowUtils.throwIf(StringUtils.isBlank(userDeptName), CodeBindMessageEnums.PARAMS_ERROR, "缺少系部名称");
        }

        // 不允许添加相同角色并且名字相同的用户
        User aUser = userService.getOne(new QueryWrapper<User>()
                .eq("userName", userName)
                .eq("userRole", userRole)
        );
        ThrowUtils.throwIf(aUser != null, CodeBindMessageEnums.PARAMS_ERROR, "不允许添加相同角色的同名用户, 请不要重复添加, 请加上数字后缀避免相同");

        // 如果有选择专业则必须选择系部所属的专业
        String userProject = request.getProject();
        if (StringUtils.isNotBlank(userProject)) {
            Project project = projectService.getOne(new QueryWrapper<Project>().eq("projectName", userProject));
            ThrowUtils.throwIf(project == null, CodeBindMessageEnums.PARAMS_ERROR, "所选专业不存在");
            assert project != null;
            ThrowUtils.throwIf(!project.getDeptName().equals(userDeptName), CodeBindMessageEnums.PARAMS_ERROR, "[" + project.getProjectName() + "] 专业属于 [" + project.getDeptName() + "] 系部, 请正确选择系部和专业");
        }

        // 如果是添加学生则需要检查是否设置了系部和专业
        if (userRoleEnum == UserRoleEnum.STUDENT) {
            ThrowUtils.throwIf(StringUtils.isBlank(userDeptName), CodeBindMessageEnums.PARAMS_ERROR, "缺少系部名称");
            ThrowUtils.throwIf(StringUtils.isBlank(userProject), CodeBindMessageEnums.PARAMS_ERROR, "缺少专业名称");
        }
        // 如果是添加主任或教师则需要检查是否设置了系部和专业
        else if (userRoleEnum == UserRoleEnum.DEPT || userRoleEnum == UserRoleEnum.TEACHER) {
            ThrowUtils.throwIf(StringUtils.isBlank(userDeptName), CodeBindMessageEnums.PARAMS_ERROR, "缺少系部名称");
        } else if (userRoleEnum == UserRoleEnum.ADMIN) {
            log.info("添加管理员");
        }
        // 兜底情况
        else {
            ThrowUtils.throwIf(true, CodeBindMessageEnums.SYSTEM_ERROR, "系统发生未知情况，请联系系统管理员");
        }

        // 创建新的用户实例。临时密码只在本次响应中展示，服务端仅保存 BCrypt 散列
        String temporaryPassword = passwordService.generateTemporaryPassword();
        User user = new User();
        BeanUtils.copyProperties(request, user);
        user.setUserAccount(normalizedUserAccount);
        user.setDept(userDeptName);
        user.setProject(userProject);
        user.setUserRole(userRole);
        user.setStatus(null);
        user.setUserPassword(passwordService.encodePassword(temporaryPassword));

        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, CodeBindMessageEnums.OPERATION_ERROR, "添加新的用户失败");
        return new BaseResponse<>(
                CodeBindMessageEnums.SUCCESS.getCode(),
                "成功；临时密码（仅显示一次）：" + temporaryPassword,
                user.getId()
        );
    }

    /**
     * 在事务中按 User -> Topic -> StudentTopicSelection 悲观锁顺序释放课题余量、清理关联数据并踢出用户会话
     *
     * @param request 删除用户请求
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteUser(DeleteRequest request) {
        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        String userAccount = request.getUserAccount();
        ThrowUtils.throwIf(StringUtils.isBlank(userAccount), CodeBindMessageEnums.PARAMS_ERROR, "指定删除的用户账号不能为空");

        User user = userService.userIsExist(userAccount);
        ThrowUtils.throwIf(user == null, CodeBindMessageEnums.PARAMS_ERROR, "用户不存在无需删除");
        assert user != null;

        // 不允许删除超级管理员
        Long id = user.getId();
        ThrowUtils.throwIf(id == 1L, CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "无法删除超级管理员");

        // 如果删除的是教师, 则需要把该教师关联的选题记录删除, 同时清空选题关联记录(无论是否预选)
        if (userService.userIsTeacher(user)) {
            List<Topic> topicList = topicService.list(
                    new QueryWrapper<Topic>().eq("teacherAccount", user.getUserAccount())
            );
            List<Long> topicIds = topicList
                    .stream()
                    .map(Topic::getId)
                    .collect(Collectors.toList());

            if (!topicIds.isEmpty()) {
                studentTopicSelectionService.remove(new QueryWrapper<StudentTopicSelection>().in("topicId", topicIds));
                topicService.removeByIds(topicIds);
            }
        }

        // 如果删除的是学生, 则需要清空选题关联记录(无论是否预选)
        if (userService.userIsStudent(user)) {
            User lockedStudent = userMapper.selectByIdForUpdate(user.getId());
            ThrowUtils.throwIf(lockedStudent == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "用户不存在无需删除");

            List<StudentTopicSelection> selections = studentTopicSelectionService.list(
                    new QueryWrapper<StudentTopicSelection>().eq("userAccount", userAccount)
            );
            if (!selections.isEmpty()) {
                List<Long> topicIds = selections
                        .stream()
                        .map(StudentTopicSelection::getTopicId)
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());

                Map<Long, Topic> lockedTopics = new HashMap<>();
                for (Long topicId : topicIds) {
                    Topic topic = topicMapper.selectByIdForUpdate(topicId);
                    if (topic != null) {
                        lockedTopics.put(topicId, topic);
                    }
                }

                List<StudentTopicSelection> lockedSelections = studentTopicSelectionMapper.selectByUserForUpdate(userAccount);
                for (StudentTopicSelection selection : lockedSelections) {
                    Topic topic = lockedTopics.get(selection.getTopicId());
                    if (topic == null || !isActiveSelection(selection)) {
                        continue;
                    }
                    restoreTopicCounters(topic, selection);
                    boolean updated = topicService.updateById(topic);
                    ThrowUtils.throwIf(!updated, CodeBindMessageEnums.OPERATION_ERROR, "无法恢复题目余量");
                }
            }
        }

        // 删除用户
        boolean result = userService.removeById(user.getId());
        ThrowUtils.throwIf(!result, CodeBindMessageEnums.SYSTEM_ERROR, "删除用户失败");

        // 删除用户所选的选题关联记录
        studentTopicSelectionService.remove(new QueryWrapper<StudentTopicSelection>().eq("userAccount", userAccount));
        StpUtil.logout(user.getId());
        return result;
    }

    /**
     * 校验角色变更前置条件并在事务中更新用户记录，若角色发生改变则踢出该用户当前登录态
     *
     * @param request 更新用户请求
     * @return 是否更新成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateUser(UserUpdateRequest request) {
        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        Long id = request.getId();
        ThrowUtils.throwIf(id == null || id <= 0, CodeBindMessageEnums.PARAMS_ERROR, "用户标识不合法, 必须为正整数");

        User oldUser = userService.getById(id);
        ThrowUtils.throwIf(oldUser == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "用户不存在");

        Integer newRole = request.getUserRole();
        if (newRole != null) {
            ThrowUtils.throwIf(UserRoleEnum.getEnums(newRole) == null, CodeBindMessageEnums.PARAMS_ERROR, "该用户角色不存在");
            ThrowUtils.throwIf(
                    (Objects.equals(newRole, UserRoleEnum.DEPT.getCode())
                            || Objects.equals(newRole, UserRoleEnum.TEACHER.getCode()))
                            && StringUtils.isBlank(oldUser.getDept()),
                    CodeBindMessageEnums.PARAMS_ERROR,
                    "专业负责人或教师账号必须先配置所属系部"
            );
            ThrowUtils.throwIf(
                    Objects.equals(newRole, UserRoleEnum.STUDENT.getCode())
                            && (StringUtils.isBlank(oldUser.getDept()) || StringUtils.isBlank(oldUser.getProject())),
                    CodeBindMessageEnums.PARAMS_ERROR,
                    "学生账号必须先配置所属系部和专业"
            );
        }
        boolean roleChanged = newRole != null && !Objects.equals(oldUser.getUserRole(), newRole);

        // 创建更新后的新用户实例
        User user = new User();
        BeanUtils.copyProperties(request, user);

        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, CodeBindMessageEnums.OPERATION_ERROR, "更新失败");
        if (roleChanged) {
            StpUtil.logout(id);
        }
        return true;
    }

    /// 用户只读查询用例 ///

    /**
     * 获取当前登录用户并转换为 LoginUserVO
     *
     * @return 当前登录用户脱敏视图对象
     */
    @Override
    public LoginUserVO getLoginUser() {
        User user = userService.userGetCurrentLoginUser();
        return userService.getLoginUserVO(user);
    }

    /**
     * 校验分页参数与教师/管理员权限边界后调用 UserService 分页查询
     *
     * @param request 用户分页查询请求
     * @return 用户分页数据
     */
    @Override
    public Page<User> listUserByPage(UserQueryRequest request) {
        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;
        long current = request.getCurrent();
        ThrowUtils.throwIf(current < 1, CodeBindMessageEnums.PARAMS_ERROR, "页码号必须大于 0");

        long size = request.getPageSize();
        ThrowUtils.throwIf(size < 1 || size > 100, CodeBindMessageEnums.PARAMS_ERROR, "页大小必须在 1 到 100 之间");

        User loginUser = userService.userGetCurrentLoginUser();
        Integer requestedRole = request.getUserRole();
        if (!userService.userIsAdmin(loginUser)) {
            requireDepartment(loginUser);
            ThrowUtils.throwIf(
                    !userService.userIsTeacher(loginUser)
                            || !Objects.equals(requestedRole, UserRoleEnum.STUDENT.getCode()),
                    CodeBindMessageEnums.NO_AUTH_ERROR,
                    "教师只能查看本系学生列表"
            );
        }

        // 获取搜索条件
        QueryWrapper<User> queryWrapper = userService.getQueryWrapper(request);
        if (!userService.userIsAdmin(loginUser)) {
            queryWrapper.eq("dept", loginUser.getDept());
        }

        // 获取用户数据
        return userService.page(new Page<>(current, size), queryWrapper);
    }

    /**
     * 校验目标角色并按当前教师所属系部过滤后组装 TeacherVO 下拉列表
     *
     * @param request 教师查询请求
     * @return 教师脱敏下拉列表数据
     */
    @Override
    public List<TeacherVO> getTeacher(TeacherQueryRequest request) {
        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        Integer userRole = request.getUserRole();
        ThrowUtils.throwIf(userRole == null, CodeBindMessageEnums.PARAMS_ERROR, "用户角色不能为空");

        UserRoleEnum userRoleEnum = UserRoleEnum.getEnums(userRole);
        ThrowUtils.throwIf(userRoleEnum == null, CodeBindMessageEnums.PARAMS_ERROR, "该用户角色不存在");
        assert userRoleEnum != null;

        User loginUser = userService.userGetCurrentLoginUser();
        requireDepartment(loginUser);

        // 获取所有的教师数据（如果当前登陆用户是教师且查询的是主任就只能查询和自己同系部的主任）
        List<User> userList = userService.list(
                new QueryWrapper<User>()
                        .eq("userRole", userRoleEnum.getCode())
                        .eq(loginUser.getUserRole().equals(UserRoleEnum.TEACHER.getCode()), "dept", loginUser.getDept())
        );
        List<TeacherVO> teacherVOList = new ArrayList<>();
        for (User user : userList) {
            TeacherVO teacherVO = new TeacherVO();
            final String userName = user.getUserName();
            teacherVO.setLabel(userName);
            teacherVO.setValue(userName);
            teacherVOList.add(teacherVO);
        }
        return teacherVOList;
    }

    /**
     * 校验 id 为正整数后通过 UserService 查询用户实体
     *
     * @param id 用户 id
     * @return 用户实体数据
     */
    @Override
    public User getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, CodeBindMessageEnums.PARAMS_ERROR, "用户标识必须是正整数");
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "不存在该用户");
        return user;
    }

    /**
     * 查询用户实体并转换为脱敏 UserVO
     *
     * @param id 用户 id
     * @return 用户脱敏视图对象
     */
    @Override
    public UserVO getUserVOById(long id) {
        User user = getUserById(id);
        return userService.getUserVO(user);
    }

    /// 私有辅助方法 ///

    /**
     * 判断学生选题记录是否为已确认的最终选题状态
     *
     * @param selection 学生选题关联实体
     * @return 是否为最终选题
     */
    private boolean isFinalSelection(StudentTopicSelection selection) {
        return selection != null
                && Objects.equals(selection.getStatus(), StudentTopicSelectionStatusEnum.EN_SELECT.getCode());
    }

    /**
     * 判断学生选题记录是否为有效占用状态（含预选与最终确认）
     *
     * @param selection 学生选题关联实体
     * @return 是否为有效选题记录
     */
    private boolean isActiveSelection(StudentTopicSelection selection) {
        return selection != null
                && (Objects.equals(selection.getStatus(), StudentTopicSelectionStatusEnum.EN_PRESELECT.getCode())
                || isFinalSelection(selection));
    }

    /**
     * 根据释放的选题记录恢复题目的剩余余量与已选计数
     *
     * @param topic     选题实体
     * @param selection 学生选题关联实体
     */
    private void restoreTopicCounters(Topic topic, StudentTopicSelection selection) {
        if (!isActiveSelection(selection)) {
            return;
        }
        if (isFinalSelection(selection)) {
            topic.setSurplusQuantity(topic.getSurplusQuantity() + 1);
        }
        topic.setSelectAmount(Math.max(0, topic.getSelectAmount() - 1));
    }

    /**
     * 校验并获取用户配置的所属系部名称
     *
     * @param user 用户实体
     * @return 系部名称
     */
    private String requireDepartment(User user) {
        String dept = user == null ? null : StringUtils.trim(user.getDept());
        ThrowUtils.throwIf(StringUtils.isBlank(dept), CodeBindMessageEnums.NO_AUTH_ERROR, "当前账号未配置所属系部");
        return dept;
    }

}
