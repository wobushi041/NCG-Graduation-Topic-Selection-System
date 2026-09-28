package cn.edu.nfu.topicselection.service.impl;

import cn.edu.nfu.topicselection.constant.CommonConstant;
import cn.edu.nfu.topicselection.constant.TopicConstant;
import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import cn.edu.nfu.topicselection.model.entity.Project;
import cn.edu.nfu.topicselection.model.entity.StudentTopicSelection;
import cn.edu.nfu.topicselection.model.entity.Topic;
import cn.edu.nfu.topicselection.model.entity.User;
import cn.edu.nfu.topicselection.model.enums.StudentTopicSelectionStatusEnum;
import cn.edu.nfu.topicselection.model.enums.TopicStatusEnum;
import cn.edu.nfu.topicselection.model.enums.UserRoleEnum;
import cn.edu.nfu.topicselection.model.request.topic.TopicQueryByAdminRequest;
import cn.edu.nfu.topicselection.model.request.topic.TopicQueryRequest;
import cn.edu.nfu.topicselection.model.request.user.DeptTeacherQueryRequest;
import cn.edu.nfu.topicselection.model.request.user.GetUserListRequest;
import cn.edu.nfu.topicselection.model.request.user.UserQueryRequest;
import cn.edu.nfu.topicselection.model.vo.DeptTeacherVO;
import cn.edu.nfu.topicselection.model.vo.SituationVO;
import cn.edu.nfu.topicselection.model.vo.UserNameVO;
import cn.edu.nfu.topicselection.model.vo.UserVO;
import cn.edu.nfu.topicselection.service.ProjectService;
import cn.edu.nfu.topicselection.service.SelectionReportService;
import cn.edu.nfu.topicselection.service.StudentTopicSelectionService;
import cn.edu.nfu.topicselection.service.SwitchService;
import cn.edu.nfu.topicselection.service.TopicService;
import cn.edu.nfu.topicselection.service.UserService;
import cn.edu.nfu.topicselection.utils.SqlUtils;
import cn.edu.nfu.topicselection.utils.ThrowUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 选题题目与统计报表只读查询服务实现类
 *
 * @author wobushi041
 */
@Service
public class SelectionReportServiceImpl implements SelectionReportService {

    /**
     * 注入用户服务依赖
     */
    private final UserService userService;

    /**
     * 注入专业服务依赖
     */
    private final ProjectService projectService;

    /**
     * 注入选题服务依赖
     */
    private final TopicService topicService;

    /**
     * 注入学生选题关联服务依赖
     */
    private final StudentTopicSelectionService studentTopicSelectionService;

    /**
     * 注入开关服务依赖
     */
    private final SwitchService switchService;

    /**
     * 构造选题题目与统计报表只读查询服务实现类实例
     *
     * @param userService                  用户服务
     * @param projectService               专业服务
     * @param topicService                 选题服务
     * @param studentTopicSelectionService 学生选题关联服务
     * @param switchService                开关服务
     */
    public SelectionReportServiceImpl(
            UserService userService,
            ProjectService projectService,
            TopicService topicService,
            StudentTopicSelectionService studentTopicSelectionService,
            SwitchService switchService
    ) {
        this.userService = userService;
        this.projectService = projectService;
        this.topicService = topicService;
        this.studentTopicSelectionService = studentTopicSelectionService;
        this.switchService = switchService;
    }

    /// 选题与用户统计只读查询服务实现 ///

    /**
     * 校验分页参数并结合当前登录角色（管理员/系主任/教师/学生）与系统开关组装 MyBatis-Plus 条件分页查询题目表
     *
     * @param request 选题分页查询请求
     * @return 选题分页数据
     */
    @Override
    public Page<Topic> getTopicList(TopicQueryRequest request) {
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
            queryWrapper.in("status", TopicStatusEnum.NOT_PUBLISHED.getCode(), TopicStatusEnum.PUBLISHED.getCode());
        }

        // 获取选题数据
        return topicService.page(new Page<>(current, size), queryWrapper);
    }

    /**
     * 根据当前登录用户角色与系部范围查询学生表与选题关联表并汇总计算已选及未选人数
     *
     * @return 选题统计情况视图对象
     */
    @Override
    public SituationVO getSelectTopicSituation() {
        // 获取当前登陆用户
        User loginUser = userService.userGetCurrentLoginUser();
        if (userService.userIsDept(loginUser)) {
            requireDepartment(loginUser);
        }

        // 获取总人数
        QueryWrapper<User> queryWrapper = new QueryWrapper<User>()
                .eq("userRole", UserRoleEnum.STUDENT.getCode())
                .eq(!userService.userIsAdmin(loginUser), "dept", loginUser.getDept())
                ;
        int totalStudents = (int) userService.count(queryWrapper);
        List<User> userList = userService.list(queryWrapper);

        // 获取已选题人数
        int selectedStudents = 0;
        for (User user : userList) {
            final String userAccount = user.getUserAccount();
            selectedStudents += (int) studentTopicSelectionService
                    .count(new QueryWrapper<StudentTopicSelection>()
                            .eq("userAccount", userAccount)
                            .eq("status", StudentTopicSelectionStatusEnum.EN_SELECT.getCode())
                    );
        }

        // 获取未选题人数
        int unselectedStudents = totalStudents - selectedStudents;

        // 封装返回数据
        SituationVO situationVO = new SituationVO();
        situationVO.setAmount(totalStudents);
        situationVO.setSelectAmount(selectedStudents);
        situationVO.setUnselectAmount(unselectedStudents);
        return situationVO;
    }

    /**
     * 校验分页与跨选开关后查询教师列表并逐个汇总已审核通过或已发布题目的余量与已选数量进行内存分页
     *
     * @param request 系部教师分页查询请求
     * @return 系部教师统计分页数据
     */
    @Override
    public Page<DeptTeacherVO> getTeacher(DeptTeacherQueryRequest request) {
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
        userQueryWrapper.eq("userRole", UserRoleEnum.TEACHER.getCode());

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

        List<User> users = userService.list(userQueryWrapper);

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
                    .in("status", TopicStatusEnum.NOT_PUBLISHED.getCode(), TopicStatusEnum.PUBLISHED.getCode());
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
        teacherPage.setTotal((long) total);
        return teacherPage;
    }

    /**
     * 校验当前系主任所属系部后查询同系部全体学生并过滤排除已处于生效选题记录中的学生账号
     *
     * @return 同系部未选题学生列表
     */
    @Override
    public List<User> getUnSelectTopicStudentList() {
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
        return userList.stream().filter(user -> !selectedUserAccounts.contains(user.getUserAccount())).collect(Collectors.toList());
    }

    /**
     * 校验分页参数并委托 TopicService 构建管理员查询条件分页查询题目表
     *
     * @param request 管理员查询题目分页请求
     * @return 选题分页数据
     */
    @Override
    public Page<Topic> getTopicListByAdmin(TopicQueryByAdminRequest request) {
        // 参数检查
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        long current = request.getCurrent();
        ThrowUtils.throwIf(current < 1, CodeBindMessageEnums.PARAMS_ERROR, "页码必须大于等于 1");

        long size = request.getPageSize();
        ThrowUtils.throwIf(size < 1 || size > 100, CodeBindMessageEnums.PARAMS_ERROR, "页大小必须在 1 到 100 之间");

        return topicService.page(new Page<>(current, size), topicService.getTopicQueryByAdminWrapper(request));
    }

    /**
     * 校验分页参数（每页不超过 20 条）后通过 UserService 分页查询用户并转换为脱敏视图分页对象
     *
     * @param request 用户分页查询请求
     * @return 用户脱敏视图分页数据
     */
    @Override
    public Page<UserVO> listUserVOByPage(UserQueryRequest request) {
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
        return userVOPage;
    }

    /**
     * 校验用户角色枚举合法性后按角色编码查询用户表并组装 UserNameVO 列表
     *
     * @param request 获取用户姓名列表请求
     * @return 用户姓名视图列表
     */
    @Override
    public List<UserNameVO> getUserList(GetUserListRequest request) {
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
        return userNameVO;
    }

    /**
     * 校验当前系主任所属系部后查询本系部教师列表并汇总待审核状态题目的余量与已选数量进行内存分页
     *
     * @param request 系部教师查询请求
     * @return 待审核题目的系部教师分页数据
     */
    @Override
    public Page<DeptTeacherVO> getTeacherByAdmin(DeptTeacherQueryRequest request) {
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
        teacherPage.setTotal((long) total);
        return teacherPage;
    }

    /// 私有辅助方法 ///

    /**
     * 校验并获取用户所属专业的选题组名称
     *
     * @param user 用户实体
     * @return 专业所属选题组名称
     */
    private String requireUserGroup(User user) {
        Project project = projectService.getOne(new QueryWrapper<Project>().eq("projectName", user.getProject()));
        ThrowUtils.throwIf(project == null || StringUtils.isBlank(project.getGroupName()),
                CodeBindMessageEnums.NO_AUTH_ERROR, "当前专业未配置选题组");
        return project.getGroupName();
    }

    /**
     * 校验并获取用户配置的所属系部名称
     *
     * @param user 用户实体
     * @return 系部名称
     */
    private static String requireDepartment(User user) {
        String dept = user == null ? null : StringUtils.trim(user.getDept());
        ThrowUtils.throwIf(StringUtils.isBlank(dept), CodeBindMessageEnums.NO_AUTH_ERROR, "当前账号未配置所属系部");
        return dept;
    }

}
