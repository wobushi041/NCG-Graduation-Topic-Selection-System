package cn.com.edtechhub.worktopicselection.controller;

import cn.com.edtechhub.worktopicselection.constant.CommonConstant;
import cn.com.edtechhub.worktopicselection.constant.TopicConstant;
import cn.com.edtechhub.worktopicselection.constant.UserConstant;
import cn.com.edtechhub.worktopicselection.exception.CodeBindMessageEnums;
import cn.com.edtechhub.worktopicselection.manager.redis.RedisManager;
import cn.com.edtechhub.worktopicselection.manager.sentine.SentineManager;
import cn.com.edtechhub.worktopicselection.mapper.StudentTopicSelectionMapper;
import cn.com.edtechhub.worktopicselection.mapper.TopicMapper;
import cn.com.edtechhub.worktopicselection.mapper.UserMapper;
import cn.com.edtechhub.worktopicselection.model.dto.dept.DeleteDeptRequest;
import cn.com.edtechhub.worktopicselection.model.dto.dept.DeptAddRequest;
import cn.com.edtechhub.worktopicselection.model.dto.dept.DeptQueryRequest;
import cn.com.edtechhub.worktopicselection.model.dto.dept.SetDeptConfigRequest;
import cn.com.edtechhub.worktopicselection.model.dto.project.DeleteProjectRequest;
import cn.com.edtechhub.worktopicselection.model.dto.project.ProjectAddRequest;
import cn.com.edtechhub.worktopicselection.model.dto.project.ProjectQueryRequest;
import cn.com.edtechhub.worktopicselection.model.dto.project.ProjectGroupUpdateRequest;
import cn.com.edtechhub.worktopicselection.model.dto.topic.*;
import cn.com.edtechhub.worktopicselection.model.dto.user.*;
import cn.com.edtechhub.worktopicselection.model.entity.*;
import cn.com.edtechhub.worktopicselection.model.enums.StudentTopicSelectionStatusEnum;
import cn.com.edtechhub.worktopicselection.model.enums.TopicStatusEnum;
import cn.com.edtechhub.worktopicselection.model.enums.UserRoleEnum;
import cn.com.edtechhub.worktopicselection.model.vo.*;
import cn.com.edtechhub.worktopicselection.response.BaseResponse;
import cn.com.edtechhub.worktopicselection.response.TheResult;
import cn.com.edtechhub.worktopicselection.service.*;
import cn.com.edtechhub.worktopicselection.utils.SqlUtils;
import cn.com.edtechhub.worktopicselection.utils.ThrowUtils;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户与核心业务控制层
 * TODO: 管理端强制退选清除功能
 * TODO: 编写完整的单元测试
 * TODO: 可以添加一个通知所有在线用户的功能，并且支持多线程发送（就是线程不要设置太多）
 * TODO: 添加 AI 智能体阅读功能，用于辅助教师和学生查重和寻找题目
 * TODO: 修复批量导入乱码功能
 * TODO: 修复按照当前逻辑修复选题使用教师名字的问题
 * TODO: 重构 Service 层，该迁移就迁移
 * TODO: 重构接口层，提高可维护性，按道理来说，切割接口层的影响不大，有 IDEA 兜底
 * TODO: 发现了一个小 bug 无法直接创建管理员
 * TODO: 提供一个想法，考虑加一个方便管理员进行测试的一个关闭限流开关，并且可以考虑做一个网站管理配置，同时只能提供给超级管理员
 *
 * @author wobushi041
 */
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    /**
     * 注入 SentineManager 依赖
     */
    @Resource
    SentineManager sentineManager;

    /**
     * 注入事务管理依赖
     */
    @Resource
    TransactionTemplate transactionTemplate;

    /**
     * 注入 Redis 管理依赖
     */
    @Resource
    RedisManager redisManager;

    /**
     * 注入用户服务依赖
     */
    @Resource
    private UserService userService;

    /**
     * 注入密码服务依赖
     */
    @Resource
    private PasswordService passwordService;

    /**
     * 注入用户持久层依赖
     */
    @Resource
    private UserMapper userMapper;

    /**
     * 注入教师选题组服务依赖
     */
    @Resource
    private TeacherGroupService teacherGroupService;

    /**
     * 注入系部服务依赖
     */
    @Resource
    private DeptService deptService;

    /**
     * 注入专业服务依赖
     */
    @Resource
    private ProjectService projectService;

    /**
     * 注入选题服务依赖
     */
    @Resource
    private TopicService topicService;

    /**
     * 注入选题持久层依赖
     */
    @Resource
    private TopicMapper topicMapper;

    /**
     * 注入学生选题关联服务依赖
     */
    @Resource
    private StudentTopicSelectionService studentTopicSelectionService;

    /**
     * 注入学生选题关联持久层依赖
     */
    @Resource
    private StudentTopicSelectionMapper studentTopicSelectionMapper;

    /**
     * 注入邮箱服务依赖
     */
    @Resource
    private MailService mailService;

    /**
     * 注入开关服务依赖
     */
    @Resource
    private SwitchService switchService;

    /// 测试相关接口 ///

    /**
     * 测试接口
     *
     * @return 测试响应
     */
    @GetMapping("/test")
    public BaseResponse<String> test() {
        return TheResult.notyet();
    }

    /// 用户相关接口 ///

    /**
     * 创建用户接口
     *
     * @param request 创建用户请求
     * @return 新创建的用户 id
     */
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/add")
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest request) throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

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

        // 创建新的用户实例。临时密码只在本次响应中展示，服务端仅保存 BCrypt 散列。
        String temporaryPassword = passwordService.generateTemporaryPassword();
        return transactionTemplate.execute(transactionStatus -> {
            User user = new User();
            BeanUtils.copyProperties(request, user); // 数据库中的 userAccount 是主键
            user.setUserAccount(normalizedUserAccount);
            user.setDept(userDeptName);
            user.setProject(userProject);
            user.setUserRole(userRole);
            user.setStatus(null);
            user.setUserPassword(passwordService.encodePassword(temporaryPassword));

            boolean result = userService.save(user); // TODO：这里有个逻辑字段重复删除失败的问题以后解决
            ThrowUtils.throwIf(!result, CodeBindMessageEnums.OPERATION_ERROR, "添加新的用户失败");
            return new BaseResponse<>(
                    CodeBindMessageEnums.SUCCESS.getCode(),
                    "成功；临时密码（仅显示一次）：" + temporaryPassword,
                    user.getId()
            );
        });
    }

    /**
     * 删除用户接口
     *
     * @param request 删除用户请求
     * @return 是否删除成功
     */
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest request) throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        String userAccount = request.getUserAccount();
        ThrowUtils.throwIf(StringUtils.isBlank(userAccount), CodeBindMessageEnums.PARAMS_ERROR, "指定删除的用户账号不能为空");

        User user = userService.userIsExist(userAccount);
        ThrowUtils.throwIf(user == null, CodeBindMessageEnums.PARAMS_ERROR, "用户不存在无需删除");
        assert user != null;

        // 删除用户
        BaseResponse<Boolean> response = transactionTemplate.execute(transactionStatus -> {
            // 不允许删除超级管理员
            Long id = user.getId();
            ThrowUtils.throwIf(id == 1L, CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "无法删除超级管理员");

            // 如果删除的是教师, 则需要把该教师关联的选题记录删除, 同时清空选题关联记录(无论是否预选)
            if (userService.userIsTeacher(user)) {
                // 先找出该教师的所有关联题目的 id
                List<Topic> topicList = topicService.list(
                        new QueryWrapper<Topic>().eq("teacherAccount", user.getUserAccount())
                );
                List<Long> topicIds = topicList
                        .stream()
                        .map(Topic::getId)
                        .collect(Collectors.toList());

                // 如果该教师确实有题目
                if (!topicIds.isEmpty()) {
                    // 删除关联的学生选题记录
                    studentTopicSelectionService.remove(new QueryWrapper<StudentTopicSelection>().in("topicId", topicIds));
                    // 同时删除这些题目
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
            return TheResult.success(CodeBindMessageEnums.SUCCESS, result);
        });
        StpUtil.logout(user.getId());
        return response;
    }

    /**
     * 更新用户接口
     *
     * @param request 更新用户请求
     * @return 是否更新成功
     */
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/update")
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest request) throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

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
        BaseResponse<Boolean> response = transactionTemplate.execute(transactionStatus -> {
            User user = new User();
            BeanUtils.copyProperties(request, user);

            boolean result = userService.updateById(user);
            ThrowUtils.throwIf(!result, CodeBindMessageEnums.OPERATION_ERROR, "更新失败");
            return TheResult.success(CodeBindMessageEnums.SUCCESS, true);
        });
        if (roleChanged) {
            StpUtil.logout(id);
        }
        return response;
    }

    /**
     * 获取当前登录用户数据
     *
     * @return 当前登录用户脱敏视图对象
     */
    @SaCheckLogin
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser() throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        // 获取当前登录用户
        User user = userService.userGetCurrentLoginUser();
        return TheResult.success(CodeBindMessageEnums.SUCCESS, userService.getLoginUserVO(user));
    }

    /**
     * 获取用户分页数据
     *
     * @param request 用户分页查询请求
     * @return 用户分页数据
     */
    @SaCheckLogin
    @SaCheckRole(value = {"admin", "teacher"}, mode = SaMode.OR)
    @PostMapping("/get/user/page")
    public BaseResponse<Page<User>> listUserByPage(@RequestBody UserQueryRequest request) throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

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
        Page<User> userPage = userService.page(new Page<>(current, size), queryWrapper);

        return TheResult.success(CodeBindMessageEnums.SUCCESS, userPage);
    }

    /**
     * 获取所有教师的脱敏列表数据接口（教师自己查主任只能获得同系部的主任）
     *
     * @param request 教师查询请求
     * @return 教师脱敏下拉列表数据
     */
    @SaCheckLogin
    @SaCheckRole(value = {"teacher"}, mode = SaMode.OR)
    @PostMapping("/get/teacher")
    public BaseResponse<List<TeacherVO>> getTeacher(@RequestBody TeacherQueryRequest request) throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

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
        return TheResult.success(CodeBindMessageEnums.SUCCESS, teacherVOList);
    }

    /**
     * 根据 id 获取用户数据
     *
     * @param id 用户 id
     * @return 用户实体数据
     */
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @GetMapping("/get")
    public BaseResponse<User> getUserById(long id) throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        // 参数检查
        ThrowUtils.throwIf(id <= 0, CodeBindMessageEnums.PARAMS_ERROR, "用户标识必须是正整数");

        // 返回用户信息
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "不存在该用户");
        return TheResult.success(CodeBindMessageEnums.SUCCESS, user);
    }

    /**
     * 根据 id 获取用户包装数据（获取脱敏后的数据）
     *
     * @param id 用户 id
     * @return 用户脱敏视图对象
     */
    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        // 参数检查
        ThrowUtils.throwIf(id <= 0, CodeBindMessageEnums.PARAMS_ERROR, "用户标识必须是正整数");

        // 获取用户数据
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();

        // 获取脱敏后的数据
        UserVO userVO = userService.getUserVO(user);
        return TheResult.success(CodeBindMessageEnums.SUCCESS, userVO);
    }

    /// 系部专业相关接口 ///

    /**
     * 添加系部
     *
     * @param request 添加系部请求
     * @return 新添加的系部 id
     */
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/add/dept")
    public BaseResponse<Long> addDept(@RequestBody DeptAddRequest request) throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        String deptName = request.getDeptName();
        ThrowUtils.throwIf(deptName == null, CodeBindMessageEnums.PARAMS_ERROR, "系部名称不能为空");

        Dept dept = deptService.getOne(new QueryWrapper<Dept>().eq("deptName", deptName));
        ThrowUtils.throwIf(dept != null, CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "该系部已存在, 请不要重复添加");

        // 添加新的系部
        return transactionTemplate.execute(transactionStatus -> {
            Dept newDept = new Dept();
            newDept.setDeptName(deptName);
            boolean result = deptService.save(newDept);
            ThrowUtils.throwIf(!result, CodeBindMessageEnums.OPERATION_ERROR, "无法添加新的系部");
            return TheResult.success(CodeBindMessageEnums.SUCCESS, newDept.getId());
        });
    }

    /**
     * 添加专业
     *
     * @param request 添加专业请求
     * @return 新添加的专业 id
     */
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/add/project")
    public BaseResponse<Long> addProject(@RequestBody ProjectAddRequest request) throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        String projectName = request.getProjectName();
        ThrowUtils.throwIf(projectName == null, CodeBindMessageEnums.PARAMS_ERROR, "专业名称不能为空");

        String deptName = request.getDeptName();
        ThrowUtils.throwIf(deptName == null, CodeBindMessageEnums.PARAMS_ERROR, "系部名称不能为空");

        Project project = projectService.getOne(new QueryWrapper<Project>().eq("projectName", projectName));
        ThrowUtils.throwIf(project != null, CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "该专业已存在, 请不要重复添加");

        // 添加新的专业
        return transactionTemplate.execute(transactionStatus -> {
            Project newProject = new Project();
            newProject.setProjectName(projectName);
            newProject.setDeptName(deptName);
            newProject.setGroupName(StringUtils.trimToNull(request.getGroupName()));
            boolean result = projectService.save(newProject);
            ThrowUtils.throwIf(!result, CodeBindMessageEnums.OPERATION_ERROR, "无法添加新的专业");
            return TheResult.success(CodeBindMessageEnums.SUCCESS, newProject.getId());
        });
    }

    /**
     * 配置专业所属选题组
     *
     * @param request 专业选题组更新请求
     * @return 是否更新成功
     */
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/update/project/group")
    public BaseResponse<Boolean> updateProjectGroup(@RequestBody ProjectGroupUpdateRequest request) throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;
        String projectName = StringUtils.trimToNull(request.getProjectName());
        ThrowUtils.throwIf(projectName == null, CodeBindMessageEnums.PARAMS_ERROR, "专业名称不能为空");

        Project project = projectService.getOne(new QueryWrapper<Project>().eq("projectName", projectName));
        ThrowUtils.throwIf(project == null, CodeBindMessageEnums.NOT_FOUND_ERROR, "专业不存在");

        project.setGroupName(StringUtils.trimToNull(request.getGroupName()));
        boolean updated = projectService.updateById(project);
        ThrowUtils.throwIf(!updated, CodeBindMessageEnums.OPERATION_ERROR, "无法保存专业选题组");
        return TheResult.success(CodeBindMessageEnums.SUCCESS, true);
    }

    /**
     * 删除系部
     *
     * @param request 删除系部请求
     * @return 是否删除成功
     */
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/delete/dept")
    public BaseResponse<Boolean> deleteDept(@RequestBody DeleteDeptRequest request) throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        String deptName = request.getDeptName();
        ThrowUtils.throwIf(deptName == null, CodeBindMessageEnums.PARAMS_ERROR, "系部名称不能为空");

        // 保证先删除专业才能删除系部
        List<Project> projectList = projectService.list(new QueryWrapper<Project>().eq("deptName", deptName));
        if (!projectList.isEmpty()) {
            String projectNames = projectList.stream().map(Project::getProjectName).collect(Collectors.joining(", "));
            ThrowUtils.throwIf(true, CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "先删除属于该系部的所有专业（" + projectNames + "）后才能删除该系部");
        }

        // 保证先删除相关角色才能删除系部
        List<User> userList = userService.list(new QueryWrapper<User>().eq("dept", deptName));
        if (!userList.isEmpty()) {
            String userNames = userList.stream().limit(5).map(User::getUserName).collect(Collectors.joining(", "));
            if (userList.size() > 5) {
                userNames += "...";
            }
            ThrowUtils.throwIf(true, CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "先删除属于该系部的所有角色（" + userNames + "）后才能删除该系部");
        }

        // 保证先删除相关选题才能删除系部
        List<Topic> topicList = topicService.list(new QueryWrapper<Topic>().eq("deptName", deptName));
        if (!topicList.isEmpty()) {
            String topicNames = topicList.stream().limit(5).map(Topic::getTopic).collect(Collectors.joining(", "));
            if (topicList.size() > 5) {
                topicNames += "...";
            }
            ThrowUtils.throwIf(true, CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "先删除属于该系部的所有选题（" + topicNames + "）后才能删除该系部");
        }

        // 删除系部
        return transactionTemplate.execute(transactionStatus -> {
            boolean resalt = deptService.remove(new QueryWrapper<Dept>().eq("deptName", deptName));
            ThrowUtils.throwIf(!resalt, CodeBindMessageEnums.NOT_FOUND_ERROR, "找不到该系部");
            return TheResult.success(CodeBindMessageEnums.SUCCESS, true);
        });
    }

    /**
     * 删除专业
     *
     * @param request 删除专业请求
     * @return 是否删除成功
     */
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/delete/project")
    public BaseResponse<Boolean> deleteProject(@RequestBody DeleteProjectRequest request) throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        String projectName = request.getProjectName();
        ThrowUtils.throwIf(projectName == null, CodeBindMessageEnums.PARAMS_ERROR, "专业名称不能为空");

        // 保证先删除相关角色才能删除系部
        List<User> userList = userService.list(new QueryWrapper<User>().eq("project", projectName));
        if (!userList.isEmpty()) {
            String userNames = userList.stream().limit(5).map(User::getUserName).collect(Collectors.joining(", "));
            if (userList.size() > 5) {
                userNames += "...";
            }
            ThrowUtils.throwIf(true, CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "先删除属于该专业的所有角色（" + userNames + "）后才能删除该专业");
        }

        // 删除专业
        return transactionTemplate.execute(transactionStatus -> {
            boolean resalt = projectService.remove(new QueryWrapper<Project>().eq("projectName", projectName));
            ThrowUtils.throwIf(!resalt, CodeBindMessageEnums.NOT_FOUND_ERROR, "找不到该专业");
            return TheResult.success(CodeBindMessageEnums.SUCCESS, true);
        });
    }

    /**
     * 获取系部分页数据
     *
     * @param request 系部分页查询请求
     * @return 系部分页数据
     */
    @SaCheckLogin
    @PostMapping("/get/dept/page")
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    public BaseResponse<Page<Dept>> getDept(@RequestBody DeptQueryRequest request) throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        // 参数检查
        long current = request.getCurrent();
        ThrowUtils.throwIf(current < 1, CodeBindMessageEnums.PARAMS_ERROR, "页码号必须大于 0");

        long size = request.getPageSize();
        ThrowUtils.throwIf(size < 1 || size > 100, CodeBindMessageEnums.PARAMS_ERROR, "页大小必须在 1 到 100 之间");

        // 获取系部数据
        Page<Dept> deptPage = deptService.page(new Page<>(current, size), deptService.getQueryWrapper(request));
        return TheResult.success(CodeBindMessageEnums.SUCCESS, deptPage);
    }

    /**
     * 获取系部列表数据（非管理员只能获取和当前登陆用户系部相同的系部）
     *
     * @param request 系部查询请求
     * @return 系部下拉列表数据
     */
    @SaCheckLogin
    @PostMapping("/get/dept/list")
    public BaseResponse<List<DeptVO>> getDeptList(@RequestBody DeptQueryRequest request) throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");

        // 获取当前用户
        User user = userService.userGetCurrentLoginUser();

        // 获取当前用户的系部
        String deptName = user.getDept();

        // 查询所有 dept 列表
        List<Dept> deptList = deptService.list(userService.userIsAdmin(user) ? null : new QueryWrapper<Dept>().eq("deptName", deptName));

        // 脱敏数据
        List<DeptVO> deptVOList = new ArrayList<>();
        for (Dept dept : deptList) {
            DeptVO deptVO = new DeptVO();
            deptVO.setLabel(dept.getDeptName());
            deptVO.setValue(dept.getDeptName());
            deptVOList.add(deptVO);
        }
        return TheResult.success(CodeBindMessageEnums.SUCCESS, deptVOList);
    }

    /**
     * 获取专业分页数据
     *
     * @param request 专业分页查询请求
     * @return 专业分页数据
     */
    @SaCheckLogin
    @PostMapping("/get/project/page")
    public BaseResponse<Page<Project>> getProject(@RequestBody ProjectQueryRequest request) throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        // 参数检查
        long current = request.getCurrent();
        ThrowUtils.throwIf(current < 1, CodeBindMessageEnums.PARAMS_ERROR, "页码号必须大于 0");

        long size = request.getPageSize();
        ThrowUtils.throwIf(size < 1 || size > 100, CodeBindMessageEnums.PARAMS_ERROR, "页大小必须在 1 到 100 之间");

        // 获取专业数据
        Page<Project> projectPage = projectService.page(new Page<>(current, size), projectService.getQueryWrapper(request));
        return TheResult.success(CodeBindMessageEnums.SUCCESS, projectPage);
    }

    /**
     * 获取专业列表数据（非管理员只能获取和当前登陆用户系部相同的系部）
     *
     * @param request 专业查询请求
     * @return 专业下拉列表数据
     */
    @SaCheckLogin
    @PostMapping("/get/project/list")
    public BaseResponse<List<ProjectVO>> getProjectList(@RequestBody ProjectQueryRequest request) throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        long current = request.getCurrent();
        ThrowUtils.throwIf(current < 1, CodeBindMessageEnums.PARAMS_ERROR, "页码号必须大于 0");

        long size = request.getPageSize();
        ThrowUtils.throwIf(size < 1 || size > 100, CodeBindMessageEnums.PARAMS_ERROR, "页大小必须在 1 到 100 之间");

        // 获取专业数据
        Page<Project> projectPage = projectService.page(new Page<>(current, size), projectService.getQueryWrapper(request));
        List<ProjectVO> projectVOList = new ArrayList<>();
        for (Project project : projectPage.getRecords()) {
            final String projectName = project.getProjectName();
            final ProjectVO projectVO = new ProjectVO();
            projectVO.setLabel(projectName);
            projectVO.setValue(projectName);
            projectVOList.add(projectVO);
        }
        return TheResult.success(CodeBindMessageEnums.SUCCESS, projectVOList);
    }

    /// 选题题目相关接口 ///

    /**
     * 获取选题分页数据
     *
     * @param request 选题分页查询请求
     * @return 选题分页数据
     */
    @SaCheckLogin
    // @CacheSearchOptimization(ttl = 15, modelClass = Topic.class)
    @PostMapping("/get/topic/page")
    public BaseResponse<Page<Topic>> getTopicList(@RequestBody TopicQueryRequest request) throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        long current = request.getCurrent();
        ThrowUtils.throwIf(current < 1, CodeBindMessageEnums.PARAMS_ERROR, "页码号必须大于 0");

        long size = request.getPageSize();
        ThrowUtils.throwIf(size < 1 || size > 100, CodeBindMessageEnums.PARAMS_ERROR, "页大小必须在 1 到 100 之间");

        User loginUser = userService.userGetCurrentLoginUser();
        UserRoleEnum loginRole = loginUser == null || loginUser.getUserRole() == null
                ? null
                : UserRoleEnum.getEnums(loginUser.getUserRole());
        ThrowUtils.throwIf(
                loginRole == null || UserRoleEnum.BAN_ROLE.equals(loginRole),
                CodeBindMessageEnums.NO_AUTH_ERROR,
                "当前账号无权查看题目"
        );

        // 获取查询条件
        QueryWrapper<Topic> queryWrapper = topicService.getQueryWrapper(request);

        // 如果是管理员, 则支持获取没有任何人选中的题目
        if (UserRoleEnum.ADMIN.equals(loginRole)) {
            Boolean isNoOneSelectedTopic = request.getIsNoOneSelectedTopic();
            queryWrapper.eq(isNoOneSelectedTopic != null, "surplusQuantity", Boolean.TRUE.equals(isNoOneSelectedTopic) ? 1 : 0);
        } else if (UserRoleEnum.DEPT.equals(loginRole)) {
            // 如果是主任只看到本系部的选题
            queryWrapper.eq("deptName", requireDepartment(loginUser));
            queryWrapper.eq("topicGroup", requireUserGroup(loginUser));
        } else if (UserRoleEnum.TEACHER.equals(loginRole)) {
            // 如果是老师, 只看到自己负责的选题
            queryWrapper.eq("teacherAccount", loginUser.getUserAccount());
        } else if (UserRoleEnum.STUDENT.equals(loginRole)) {
            ThrowUtils.throwIf(!switchService.isEnabled(TopicConstant.VIEW_TOPIC_SWITCH), CodeBindMessageEnums.NOT_FOUND_ERROR, "当前时间学生无法查看选题, 请等待系统开放");
            // 如果是学生, 看是否开启跨选, 如果此时不允许跨选则不允许看到和当前登陆用户不同系部的教师
            if (!switchService.isEnabled(TopicConstant.CROSS_TOPIC_SWITCH)) {
                queryWrapper.eq("deptName", loginUser.getDept());
            }

            // 而且只能看到审核通过和已经发布的题目
            queryWrapper.in("status", TopicStatusEnum.NOT_PUBLISHED.getCode(), TopicStatusEnum.PUBLISHED.getCode()); // 学生只能查看已经审核通过和已经发布的选题
        }

        // 获取选题数据
        Page<Topic> topicPage = topicService.page(new Page<>(current, size), queryWrapper);

        return TheResult.success(CodeBindMessageEnums.SUCCESS, topicPage);
    }

    /**
     * 获取当前的选题情况（只能获取和当前登陆用户系部相同的选题）
     *
     * @return 选题统计情况视图对象
     */
    @SaCheckLogin
    @SaCheckRole(value = {"admin", "dept"}, mode = SaMode.OR)
    @PostMapping("/get/select/topic/situation")
    public BaseResponse<SituationVO> getSelectTopicSituation() throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        // 获取当前登陆用户
        User loginUser = userService.userGetCurrentLoginUser();
        if (userService.userIsDept(loginUser)) {
            requireDepartment(loginUser);
        }

        // 获取总人数
        QueryWrapper<User> queryWrapper = new QueryWrapper<User>()
                .eq("userRole", UserRoleEnum.STUDENT.getCode()) // 只获取学生记录
                .eq(!userService.userIsAdmin(loginUser), "dept", loginUser.getDept()) // 获取当前登陆用户系部相同的学生, 但是管理员可以获取所有学生
                ;
        int totalStudents = (int) userService.count(queryWrapper);
        List<User> userList = userService.list(queryWrapper);

        // 获取已选题人数
        int selectedStudents = 0;
        for (User user : userList) {
            final String userAccount = user.getUserAccount();
            selectedStudents += (int) studentTopicSelectionService
                    .count(new QueryWrapper<StudentTopicSelection>()
                            .eq("userAccount", userAccount) // 获取当前用户的记录
                            .eq("status", StudentTopicSelectionStatusEnum.EN_SELECT.getCode()) // 查询状态为已选题的
                    );
        }

        // 获取未选题人数
        int unselectedStudents = totalStudents - selectedStudents;

        // 封装返回数据
        SituationVO situationVO = new SituationVO();
        situationVO.setAmount(totalStudents);
        situationVO.setSelectAmount(selectedStudents);
        situationVO.setUnselectAmount(unselectedStudents);
        return TheResult.success(CodeBindMessageEnums.SUCCESS, situationVO);
    }

    /**
     * 获取系部教师数据
     *
     * @param request 系部教师分页查询请求
     * @return 系部教师统计分页数据
     */
    @SaCheckLogin
    // @CacheSearchOptimization(ttl = 30, modelClass = DeptTeacherVO.class)
    @PostMapping("/get/dept/teacher")
    public BaseResponse<Page<DeptTeacherVO>> getTeacher(@RequestBody DeptTeacherQueryRequest request) throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        long current = request.getCurrent();
        ThrowUtils.throwIf(current < 1, CodeBindMessageEnums.PARAMS_ERROR, "当前页码必须大于 0");

        long size = request.getPageSize();
        ThrowUtils.throwIf(size < 1 || size > 100, CodeBindMessageEnums.PARAMS_ERROR, "每页大小必须在 1 到 100 之间");

        String sortField = request.getSortField();
        String sortOrder = request.getSortOrder();
        String teacherName = request.getTeacherName();
        String deptName = request.getDeptName();

        // 获取当前登陆用
        User loginUser = userService.userGetCurrentLoginUser();

        // 如果是学生, 则必须检查此时是否允许允许学生查看题目
        if (userService.userIsStudent(loginUser)) {
            ThrowUtils.throwIf(!switchService.isEnabled(TopicConstant.VIEW_TOPIC_SWITCH), CodeBindMessageEnums.NOT_FOUND_ERROR, "当前时间学生无法查看选题, 请等待系统开放");
        }

        // 查询教师列表
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userRole", UserRoleEnum.TEACHER.getCode()); // 是教师的

        // 如果有教师姓名搜索条件, 则添加搜索条件
        if (StringUtils.isNotBlank(teacherName)) {
            userQueryWrapper.like("userName", teacherName);
        }

        // 如果有教师系部搜索条件, 则添加搜索条件
        if (StringUtils.isNotBlank(deptName)) {
            userQueryWrapper.like("dept", deptName);
        }

        // 如果此时不允许跨选则不允许看到和当前登陆用户不同系部的教师
        if (!switchService.isEnabled(TopicConstant.CROSS_TOPIC_SWITCH)) {
            userQueryWrapper.eq("dept", loginUser.getDept());
        }

        // 添加排序条件
        if (SqlUtils.validSortField(sortField)) {
            userQueryWrapper.orderBy(true, sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        }

        List<User> users = userService.list(userQueryWrapper); // 得到所有的教师

        // 遍历教师列表来创建返回的 Page 对象, 填充每位教师的选题情况
        List<DeptTeacherVO> teacherVOList = new ArrayList<>();
        for (User user : users) {
            // 获得教师的名字
            String userName = user.getUserName();

            // 获得教师系部
            String dept = user.getDept();

            // 获得教师的对应选题
            QueryWrapper<Topic> topicQueryWrapper = new QueryWrapper<>();
            topicQueryWrapper
                    .eq("teacherAccount", user.getUserAccount())
                    .in("status", TopicStatusEnum.NOT_PUBLISHED.getCode(), TopicStatusEnum.PUBLISHED.getCode()); // 学生只能查看已经审核通过和已经发布的选题
            int count = (int) topicService.count(topicQueryWrapper);
            List<Topic> topicList = topicService.list(topicQueryWrapper);

            // 计算剩余数量和选择数量
            Integer selectAmount = 0;
            Integer surplusQuantity = 0;
            for (Topic topic : topicList) {
                selectAmount += topic.getSelectAmount();
                surplusQuantity += topic.getSurplusQuantity();
            }

            // 构建 DeptTeacherVO 对象
            if (count != 0) {
                DeptTeacherVO teacherVO = new DeptTeacherVO();
                teacherVO.setTeacherName(userName);
                teacherVO.setDeptName(dept);
                teacherVO.setSurplusQuantity(surplusQuantity);
                teacherVO.setSelectAmount(selectAmount);
                teacherVO.setTopicAmount(count);
                teacherVOList.add(teacherVO);
            }
        }

        // 对教师列表进行分页处理
        int total = teacherVOList.size();
        int fromIndex = (int) ((current - 1) * size);
        int toIndex = (int) Math.min(fromIndex + size, total);

        // 确保索引不越界
        List<DeptTeacherVO> pagedTeacherVOList = new ArrayList<>();
        if (fromIndex < total) {
            pagedTeacherVOList = teacherVOList.subList(fromIndex, toIndex);
        }

        // 构建分页对象
        Page<DeptTeacherVO> teacherPage = new Page<>(current, size);
        teacherPage.setRecords(pagedTeacherVOList);
        teacherPage.setTotal((long) total); // 设置总记录数
        return TheResult.success(CodeBindMessageEnums.SUCCESS, teacherPage);
    }

    /**
     * 获取和当前登陆用户同系的没有选题的学生
     *
     * @return 同系部未选题学生列表
     */
    @SaCheckLogin
    @SaCheckRole(value = {"dept"}, mode = SaMode.OR)
    @PostMapping("/get/unselect/topic/student/list")
    public BaseResponse<List<User>> getUnSelectTopicStudentList() throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        // 获取当前登陆用户
        User loginUser = userService.userGetCurrentLoginUser();
        final String dept = requireDepartment(loginUser);

        // 获取所有学生用户
        final List<User> userList = userService.list(new QueryWrapper<User>().eq("userRole", UserRoleEnum.STUDENT.getCode()).eq(StringUtils.isNotBlank(dept), "dept", dept));

        // 获取所有已经选题的学生
        final List<StudentTopicSelection> selectedList = studentTopicSelectionService.list(
                new QueryWrapper<StudentTopicSelection>()
                        .eq("status", StudentTopicSelectionStatusEnum.EN_SELECT.getCode())
        );

        // 将已经选题的学生账号存入一个 Set
        Set<String> selectedUserAccounts = selectedList.stream().map(StudentTopicSelection::getUserAccount).collect(Collectors.toSet());

        // 筛选出未选题的学生
        List<User> unselectedUsers = userList.stream().filter(user -> !selectedUserAccounts.contains(user.getUserAccount())).collect(Collectors.toList());

        return TheResult.success(CodeBindMessageEnums.SUCCESS, unselectedUsers);
    }

    /**
     * 管理员获取题目
     *
     * @param request 管理员查询题目分页请求
     * @return 选题分页数据
     */
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/get/topic/list/by/admin")
    public BaseResponse<Page<Topic>> getTopicListByAdmin(@RequestBody TopicQueryByAdminRequest request) throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        long current = request.getCurrent();
        ThrowUtils.throwIf(current < 1, CodeBindMessageEnums.PARAMS_ERROR, "页码必须大于等于 1");

        long size = request.getPageSize();
        ThrowUtils.throwIf(size < 1 || size > 100, CodeBindMessageEnums.PARAMS_ERROR, "页大小必须在 1 到 100 之间");

        Page<Topic> topicPage = topicService.page(new Page<>(current, size), topicService.getTopicQueryByAdminWrapper(request));
        return TheResult.success(CodeBindMessageEnums.SUCCESS, topicPage);

    }

    /**
     * 分页获取用户封装列表
     *
     * @param request 用户分页查询请求
     * @return 用户脱敏视图分页数据
     */
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest request) throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        long size = request.getPageSize();
        ThrowUtils.throwIf(size < 1 || size > 20, CodeBindMessageEnums.PARAMS_ERROR, "页大小必须在 1 到 20 之间");

        long current = request.getCurrent();
        ThrowUtils.throwIf(current < 1, CodeBindMessageEnums.PARAMS_ERROR, "页号必须大于等于 1");

        // 查询分页结果
        Page<User> userPage = userService.page(new Page<>(current, size), userService.getQueryWrapper(request));
        Page<UserVO> userVOPage = new Page<>(current, size, userPage.getTotal());
        List<UserVO> userVO = userService.getUserVO(userPage.getRecords());
        userVOPage.setRecords(userVO);
        return TheResult.success(CodeBindMessageEnums.SUCCESS, userVOPage);
    }

    /**
     * 获取用户列表数据
     *
     * @param request 获取用户姓名列表请求
     * @return 用户姓名视图列表
     */
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/get/user/list")
    public BaseResponse<List<UserNameVO>> getUserList(@RequestBody GetUserListRequest request) throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        Integer userRole = request.getUserRole();
        ThrowUtils.throwIf(userRole == null || UserRoleEnum.getEnums(userRole) == null, CodeBindMessageEnums.PARAMS_ERROR, "用户角色不存在");
        List<UserNameVO> userNameVO = new ArrayList<>();
        List<User> userList = userService.list(new QueryWrapper<User>().eq("userRole", userRole));
        for (User user : userList) {
            UserNameVO item = new UserNameVO();
            item.setUserName(user.getUserName());
            userNameVO.add(item);
        }
        return TheResult.success(CodeBindMessageEnums.SUCCESS, userNameVO);
    }

    /**
     * 获取待审核题目的系部教师列表
     *
     * @param request 系部教师查询请求
     * @return 待审核题目的系部教师分页数据
     */
    @SaCheckLogin
    @SaCheckRole(value = {"dept"}, mode = SaMode.OR)
    @PostMapping("/get/dept/teacher/by/admin")
    public BaseResponse<Page<DeptTeacherVO>> getTeacherByAdmin(@RequestBody DeptTeacherQueryRequest request) throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        long current = request.getCurrent();
        ThrowUtils.throwIf(current < 1, CodeBindMessageEnums.PARAMS_ERROR, "页码必须大于等于 1");

        long size = request.getPageSize();
        ThrowUtils.throwIf(size < 1 || size > 100, CodeBindMessageEnums.PARAMS_ERROR, "页大小必须在 1 到 100 之间");

        String sortField = request.getSortField();
        String sortOrder = request.getSortOrder();

        User loginUser = userService.userGetCurrentLoginUser();

        String dept = requireDepartment(loginUser);

        // 查询用户列表
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("dept", dept).eq("userRole", UserRoleEnum.TEACHER.getCode()).orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        List<User> users = this.userService.list(userQueryWrapper);

        // 创建返回的 Page 对象
        List<DeptTeacherVO> teacherVOList = new ArrayList<>();
        for (User user : users) {
            String userName = user.getUserName();

            // 查询该用户的课题列表
            QueryWrapper<Topic> topicQueryWrapper = new QueryWrapper<>();
            topicQueryWrapper.eq("teacherAccount", user.getUserAccount());
            topicQueryWrapper.eq("status", TopicStatusEnum.PENDING_REVIEW.getCode());
            int count = (int) topicService.count(topicQueryWrapper);
            List<Topic> topicList = topicService.list(topicQueryWrapper);

            // 计算剩余数量和选择数量
            Integer surplusQuantity = 0;
            Integer selectAmount = 0;
            for (Topic topic : topicList) {
                surplusQuantity += topic.getSurplusQuantity();
                selectAmount += topic.getSelectAmount();
            }
            if (count != 0) {
                // 构建 DeptTeacherVO 对象
                DeptTeacherVO teacherVO = new DeptTeacherVO();
                teacherVO.setTeacherName(userName);
                teacherVO.setSurplusQuantity(surplusQuantity);
                teacherVO.setSelectAmount(selectAmount);
                teacherVO.setTopicAmount(count);
                teacherVOList.add(teacherVO);
            }
            // 添加到列表中
        }

        // 对教师列表进行分页处理
        int total = teacherVOList.size();
        int fromIndex = (int) ((current - 1) * size);
        int toIndex = (int) Math.min(fromIndex + size, total);

        // 确保索引不越界
        List<DeptTeacherVO> pagedTeacherVOList = new ArrayList<>();
        if (fromIndex < total) {
            pagedTeacherVOList = teacherVOList.subList(fromIndex, toIndex);
        }

        // 构建分页对象
        Page<DeptTeacherVO> teacherPage = new Page<>(current, size);
        teacherPage.setRecords(pagedTeacherVOList);
        teacherPage.setTotal((long) total); // 设置总记录数
        return TheResult.success(CodeBindMessageEnums.SUCCESS, teacherPage);
    }

    /**
     * 查询是否允许跨系状态
     *
     * @return 是否开启跨系选题
     */
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @GetMapping("/cross_topic")
    public BaseResponse<Boolean> getCrossTopicStatus() throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        return TheResult.success(CodeBindMessageEnums.SUCCESS, switchService.isEnabled(TopicConstant.CROSS_TOPIC_SWITCH));
    }

    /**
     * 设置是否允许跨系开关
     *
     * @param enabled 是否开启跨系选题
     * @return 操作结果提示信息
     */
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/cross_topic")
    public BaseResponse<String> setCrossTopicStatus(@RequestParam boolean enabled) throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        switchService.setEnabled(TopicConstant.CROSS_TOPIC_SWITCH, enabled);
        return TheResult.success(CodeBindMessageEnums.SUCCESS, "跨系选题功能已" + (enabled ? "开启" : "关闭"));
    }

    /**
     * 查询学生查看选题状态
     *
     * @return 是否允许学生查看选题
     */
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @GetMapping("/view_topic")
    public BaseResponse<Boolean> getViewTopicStatus() throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        return TheResult.success(CodeBindMessageEnums.SUCCESS, switchService.isEnabled(TopicConstant.VIEW_TOPIC_SWITCH));
    }

    /**
     * 设置学生查看选题开关
     *
     * @param enabled 是否允许学生查看选题
     * @return 操作结果提示信息
     */
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/view_topic")
    public BaseResponse<String> setViewTopicStatus(@RequestParam boolean enabled) throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        switchService.setEnabled(TopicConstant.VIEW_TOPIC_SWITCH, enabled);
        return TheResult.success(CodeBindMessageEnums.SUCCESS, (enabled ? "允许" : "禁止") + "学生查看选题");
    }

    /**
     * 查询单选模式切换状态
     *
     * @return 当前单选模式开关状态
     */
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @GetMapping("/switch_single_choice")
    public BaseResponse<Boolean> getSwitchSingleChoiceStatus() throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        return TheResult.success(CodeBindMessageEnums.SUCCESS, switchService.isEnabled(TopicConstant.SWITCH_SINGLE_CHOICE));
    }

    /**
     * 设置单选模式切换开关
     *
     * @param enabled 是否切换为学生单选模式
     * @return 操作结果提示信息
     */
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/switch_single_choice")
    public BaseResponse<String> setSwitchSingleChoiceStatus(@RequestParam boolean enabled) throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        switchService.setEnabled(TopicConstant.SWITCH_SINGLE_CHOICE, enabled);
        return TheResult.success(CodeBindMessageEnums.SUCCESS, "当前单选模式切换为" + (enabled ? "学生单选模式" : "教师单选模式"));
    }

    /**
     * 查询是否退选加锁状态
     *
     * @return 退选加锁状态及锁定时间视图对象
     */
    @SaCheckLogin
    @SaCheckRole(value = {"admin", "teacher", "student"}, mode = SaMode.OR)
    @GetMapping("/topic_lock")
    public BaseResponse<TopicLockVO> getTopicLock() throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        TopicLockVO topicLockVO = new TopicLockVO();
        topicLockVO.setIslock(switchService.isEnabled(TopicConstant.TOPIC_LOCK));
        topicLockVO.setLockTime(redisManager.getValue(TopicConstant.TOPIC_LOCK_TIME));

        return TheResult.success(CodeBindMessageEnums.SUCCESS, topicLockVO);
    }

    /**
     * 设置是否退选加锁开关
     *
     * @param enabled   是否开启退选加锁
     * @param timestamp 加锁截止时间戳（秒）字符串
     * @return 操作结果提示信息
     */
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/topic_lock")
    public BaseResponse<String> setTopicLock(@RequestParam boolean enabled, String timestamp) throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        if (enabled) {
            ThrowUtils.throwIf(StringUtils.isBlank(timestamp), CodeBindMessageEnums.PARAMS_ERROR, "请设置需要加锁的时间");
            long lockTimestamp;
            try {
                lockTimestamp = Long.parseLong(timestamp);
            } catch (NumberFormatException e) {
                throw new cn.com.edtechhub.worktopicselection.exception.BusinessException(CodeBindMessageEnums.PARAMS_ERROR, "加锁时间格式不正确");
            }
            ThrowUtils.throwIf(lockTimestamp <= System.currentTimeMillis() / 1000, CodeBindMessageEnums.PARAMS_ERROR, "加锁时间必须晚于当前时间");
            redisManager.setValue(TopicConstant.TOPIC_LOCK_TIME, timestamp);
            switchService.setEnabled(TopicConstant.TOPIC_LOCK, true);
        } else {
            redisManager.deleteKey(TopicConstant.TOPIC_LOCK_TIME);
            switchService.setEnabled(TopicConstant.TOPIC_LOCK, false);
        }

        return TheResult.success(CodeBindMessageEnums.SUCCESS, "当前是否退选加锁为" + (enabled ? "禁止退选题目" : "允许退选题目"));
    }

    /**
     * 查看系部选跨选配置
     *
     * @return 系部跨选配置视图对象
     */
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @GetMapping("/get/dept/config")
    public BaseResponse<DeptConfigVO> getDeptConfig() throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        DeptConfigVO deptConfigVO = new DeptConfigVO();
        if (switchService.isEnabled(TopicConstant.CROSS_TOPIC_SWITCH)) {
            Set<String> keys = redisManager.getKeysByPattern(TopicConstant.DEPT_CROSS_TOPIC_CONFIG + ":*");
            if (keys != null && !keys.isEmpty()) {
                Map<String, List<String>> enableSelectDeptsList = new HashMap<>();

                for (String key : keys) {
                    String value = redisManager.getValue(key);
                    if (value != null) {
                        // key 格式为 DEPT_CROSS_TOPIC_CONFIG:<deptId>
                        String objectDeptName = key.split(":")[1];

                        // value 存储的是 JSON 字符串，需要反序列化
                        List<String> enableSelectDepts = JSONUtil.toList(value, String.class);

                        enableSelectDeptsList.put(objectDeptName, enableSelectDepts);
                    }
                }

                deptConfigVO.setEnableSelectDeptsList(enableSelectDeptsList);
            }
        }

        return TheResult.success(CodeBindMessageEnums.SUCCESS, deptConfigVO);
    }

    /**
     * 设置系部选跨选配置
     *
     * @param request 设置系部跨选配置请求
     * @return 是否设置成功
     */
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/set/dept/config")
    public BaseResponse<Boolean> setDeptConfig(@RequestBody SetDeptConfigRequest request) throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        // 检查参数
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        // 取出没有开启跨系开关的情况
        ThrowUtils.throwIf(!switchService.isEnabled(TopicConstant.CROSS_TOPIC_SWITCH), CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "请先开启跨系开关后再配置选题规则");

        Map<String, List<String>> enableSelectDeptsList = request.getEnableSelectDeptsList();
        ThrowUtils.throwIf(enableSelectDeptsList == null || enableSelectDeptsList.isEmpty(), CodeBindMessageEnums.PARAMS_ERROR, "请至少选择一个系部后再配置");
        boolean hasRule = false;
        for (Map.Entry<String, List<String>> entry : enableSelectDeptsList.entrySet()) {
            ThrowUtils.throwIf(StringUtils.isBlank(entry.getKey()), CodeBindMessageEnums.PARAMS_ERROR, "配置中的系部名称不能为空");
            ThrowUtils.throwIf(entry.getValue() == null, CodeBindMessageEnums.PARAMS_ERROR, "系部可选范围不能为空");
            ThrowUtils.throwIf(deptService.getOne(new QueryWrapper<Dept>().eq("deptName", entry.getKey())) == null, CodeBindMessageEnums.PARAMS_ERROR, "配置中包含不存在的系部");
            for (String targetDept : entry.getValue()) {
                ThrowUtils.throwIf(StringUtils.isBlank(targetDept), CodeBindMessageEnums.PARAMS_ERROR, "可选系部名称不能为空");
                ThrowUtils.throwIf(deptService.getOne(new QueryWrapper<Dept>().eq("deptName", targetDept)) == null, CodeBindMessageEnums.PARAMS_ERROR, "配置中包含不存在的可选系部");
            }
            hasRule = hasRule || !entry.getValue().isEmpty();
        }
        ThrowUtils.throwIf(!hasRule, CodeBindMessageEnums.PARAMS_ERROR, "请至少选择一个系部后再配置");

        // 全部校验通过后再替换全量配置。
        Set<String> keys = redisManager.getKeysByPattern(TopicConstant.DEPT_CROSS_TOPIC_CONFIG + ":*");
        redisManager.deleteKeys(keys);

        // 配置规则
        for (Map.Entry<String, List<String>> entry : enableSelectDeptsList.entrySet()) {
            String objectDeptName = entry.getKey();
            List<String> enableSelectDepts = entry.getValue();
            if (!enableSelectDepts.isEmpty()) {
                redisManager.setValue(TopicConstant.DEPT_CROSS_TOPIC_CONFIG + ":" + objectDeptName, JSONUtil.toJsonStr(enableSelectDepts));
            }
        }
        return TheResult.success(CodeBindMessageEnums.SUCCESS, true);
    }

    /**
     * 清除系部选跨选配置
     *
     * @return 是否清除成功
     */
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @PostMapping("/del/dept/config")
    public BaseResponse<Boolean> delDeptConfig() throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        // 必须开启跨选开关
        ThrowUtils.throwIf(!switchService.isEnabled(TopicConstant.CROSS_TOPIC_SWITCH), CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "请先开启跨系开关后再清除跨选规则");

        // 清理所有的跨选规则
        Set<String> keys = redisManager.getKeysByPattern(TopicConstant.DEPT_CROSS_TOPIC_CONFIG + ":*");
        redisManager.deleteKeys(keys);
        return TheResult.success(CodeBindMessageEnums.SUCCESS, true);
    }

    /**
     * 查看系统的相关信息面板信息
     *
     * @return 系统统计与资源监控信息视图对象
     */
    @SaCheckLogin
    @SaCheckRole(value = {"admin"}, mode = SaMode.OR)
    @GetMapping("/get/system/info")
    public BaseResponse<TheSystemInfoVO> getSystemInfo() throws BlockException {
        // 流量控制
        String entryName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        sentineManager.initFlowRules(entryName);
        try (com.alibaba.csp.sentinel.Entry ignored = SphU.entry(entryName)) {
        }

        // 创建存储系统信息的 VO 对象
        TheSystemInfoVO theSystemInfoVo = new TheSystemInfoVO();

        // 选题信息
        theSystemInfoVo.setTotalDeptCount(userService
                .lambdaQuery()
                .eq(User::getUserRole, 2)
                .count()
        );

        theSystemInfoVo.setTotalTeacherCount(userService
                .lambdaQuery()
                .eq(User::getUserRole, 1)
                .count()
        );

        theSystemInfoVo.setTotalStudentCount(userService
                .lambdaQuery()
                .eq(User::getUserRole, 0)
                .count()
        );

        theSystemInfoVo.setLoginUserCount(userService
                .lambdaQuery()
                .eq(User::getStatus, "老用户")
                .count()
        );

        theSystemInfoVo.setAuditPassTopicCount(topicService
                .lambdaQuery()
                .eq(Topic::getStatus, 0)
                .count()
        );

        theSystemInfoVo.setAuditBackTopicCount(topicService
                .lambdaQuery()
                .eq(Topic::getStatus, -2)
                .count()
        );

        theSystemInfoVo.setAuditTopicCount(topicService
                .lambdaQuery()
                .eq(Topic::getStatus, -1)
                .count()
        );

        theSystemInfoVo.setReleaseTopicCount(topicService
                .lambdaQuery()
                .eq(Topic::getStatus, 1)
                .count()
        );

        // 系统信息
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        com.sun.management.OperatingSystemMXBean sunOsBean = (com.sun.management.OperatingSystemMXBean) osBean;

        double cpuLoad = sunOsBean.getSystemCpuLoad() * 100;
        theSystemInfoVo.setCpuUsage(new DecimalFormat("0.00").format(cpuLoad) + "%");

        long totalPhysical = sunOsBean.getTotalPhysicalMemorySize();
        long freePhysical = sunOsBean.getFreePhysicalMemorySize();
        long usedPhysical = totalPhysical - freePhysical;
        theSystemInfoVo.setMemoryUsage(formatSize(usedPhysical) + "/" + formatSize(totalPhysical));

        File root = new File("/");
        long totalDisk = root.getTotalSpace();
        long freeDisk = root.getFreeSpace();
        long usedDisk = totalDisk - freeDisk;
        theSystemInfoVo.setDiskUsage(formatSize(usedDisk) + "/" + formatSize(totalDisk));

        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long usedHeap = heapUsage.getUsed();
        long maxHeap = heapUsage.getMax();
        theSystemInfoVo.setJvmMemoryUsage(formatSize(usedHeap) + "/" + formatSize(maxHeap));

        return TheResult.success(CodeBindMessageEnums.SUCCESS, theSystemInfoVo);
    }

    /**
     * 获取当前登录教师的选题组及额度列表
     *
     * @return 当前教师的选题组信息列表
     */
    @SaCheckRole("teacher")
    @GetMapping("/teacher/groups")
    public BaseResponse<List<Map<String, Object>>> getTeacherGroups() {
        return TheResult.success(CodeBindMessageEnums.SUCCESS,
                teacherGroupService.groups(userService.userGetCurrentLoginUser().getUserAccount()));
    }

    /**
     * 批量查询指定教师的选题组额度，供教师列表展示使用（管理员可查全部，系部主任仅限本系部）
     *
     * @param request 批量查询教师选题组请求
     * @return 教师账号到选题组额度列表的映射
     */
    @SaCheckRole(value = {"admin", "dept"}, mode = SaMode.OR)
    @PostMapping("/teacher/groups/batch")
    public BaseResponse<Map<String, List<Map<String, Object>>>> getTeacherGroupsBatch(@RequestBody TeacherGroupsBatchRequest request) {
        ThrowUtils.throwIf(request == null || request.getTeacherAccounts() == null,
                CodeBindMessageEnums.PARAMS_ERROR, "教师账号列表不能为空");
        List<String> accounts = request.getTeacherAccounts();
        User loginUser = userService.userGetCurrentLoginUser();
        if (Boolean.TRUE.equals(userService.userIsDept(loginUser))) {
            List<User> deptTeachers = userService.list(new QueryWrapper<User>()
                    .eq("userRole", UserRoleEnum.TEACHER.getCode())
                    .eq("dept", loginUser.getDept()));
            Set<String> allowedAccounts = deptTeachers.stream()
                    .map(User::getUserAccount)
                    .collect(Collectors.toSet());
            accounts = accounts.stream().filter(allowedAccounts::contains).collect(Collectors.toList());
        }
        return TheResult.success(CodeBindMessageEnums.SUCCESS, teacherGroupService.groupsBatch(accounts));
    }

    /**
     * 查询系统内现有的选题组名称列表，供管理员配置专业选题组、添加专业等下拉使用
     *
     * @return 系统内现有选题组名称列表
     */
    @SaCheckRole(value = {"admin", "dept"}, mode = SaMode.OR)
    @GetMapping("/group/list")
    public BaseResponse<List<String>> getGroupList() {
        return TheResult.success(CodeBindMessageEnums.SUCCESS, teacherGroupService.allGroups());
    }

    /**
     * 校验并获取用户所属专业的选题组名称
     *
     * @param user 用户实体
     * @return 专业所属选题组名称
     */
    String requireUserGroup(User user) {
        Project project = projectService.getOne(new QueryWrapper<Project>().eq("projectName", user.getProject()));
        ThrowUtils.throwIf(project == null || StringUtils.isBlank(project.getGroupName()),
                CodeBindMessageEnums.NO_AUTH_ERROR, "当前专业未配置选题组");
        return project.getGroupName();
    }

    /**
     * 判断学生选题记录是否为已确认的最终选题状态
     *
     * @param selection 学生选题关联实体
     * @return 是否为最终选题
     */
    boolean isFinalSelection(StudentTopicSelection selection) {
        return selection != null
                && Objects.equals(selection.getStatus(), StudentTopicSelectionStatusEnum.EN_SELECT.getCode());
    }

    /**
     * 判断学生选题记录是否为有效占用状态（含预选与最终确认）
     *
     * @param selection 学生选题关联实体
     * @return 是否为有效选题记录
     */
    boolean isActiveSelection(StudentTopicSelection selection) {
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
    void restoreTopicCounters(Topic topic, StudentTopicSelection selection) {
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
    static String requireDepartment(User user) {
        String dept = user == null ? null : StringUtils.trim(user.getDept());
        ThrowUtils.throwIf(StringUtils.isBlank(dept), CodeBindMessageEnums.NO_AUTH_ERROR, "当前账号未配置所属系部");
        return dept;
    }

    /**
     * 格式化字节容量单位
     *
     * @param size 字节大小
     * @return 格式化后的带单位容量字符串
     */
    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        int exp = (int) (Math.log(size) / Math.log(1024));
        char unit = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", size / Math.pow(1024, exp), unit);
    }

    /**
     * 将 JSON 字符串列表格式化为顿号分隔的字符串
     *
     * @param raw 原始 JSON 数组字符串
     * @return 格式化后的字符串
     */
    private String beautifyList(String raw) {
        List<String> list = JSONUtil.toList(raw, String.class);
        return String.join("、", list);
    }

}
