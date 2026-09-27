package cn.com.edtechhub.worktopicselection.service.impl;

import cn.com.edtechhub.worktopicselection.constant.UserConstant;
import cn.com.edtechhub.worktopicselection.exception.BusinessException;
import cn.com.edtechhub.worktopicselection.exception.CodeBindMessageEnums;
import cn.com.edtechhub.worktopicselection.model.entity.Dept;
import cn.com.edtechhub.worktopicselection.model.entity.Project;
import cn.com.edtechhub.worktopicselection.model.entity.StudentTopicSelection;
import cn.com.edtechhub.worktopicselection.model.entity.Topic;
import cn.com.edtechhub.worktopicselection.model.entity.User;
import cn.com.edtechhub.worktopicselection.model.enums.StudentTopicSelectionStatusEnum;
import cn.com.edtechhub.worktopicselection.model.enums.UserRoleEnum;
import cn.com.edtechhub.worktopicselection.model.request.file.UploadFileRequest;
import cn.com.edtechhub.worktopicselection.response.BaseResponse;
import cn.com.edtechhub.worktopicselection.response.TheResult;
import cn.com.edtechhub.worktopicselection.service.DeptService;
import cn.com.edtechhub.worktopicselection.service.FileApplicationService;
import cn.com.edtechhub.worktopicselection.service.PasswordService;
import cn.com.edtechhub.worktopicselection.service.ProjectService;
import cn.com.edtechhub.worktopicselection.service.SqlExportService;
import cn.com.edtechhub.worktopicselection.service.StudentTopicSelectionService;
import cn.com.edtechhub.worktopicselection.service.TopicService;
import cn.com.edtechhub.worktopicselection.service.UserService;
import cn.com.edtechhub.worktopicselection.utils.ThrowUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 文件批量导入与统计导出应用服务实现类
 *
 * @author wobushi041
 */
@Service
public class FileApplicationServiceImpl implements FileApplicationService {

    /**
     * 注入 SQL 导出服务依赖
     */
    private final SqlExportService sqlExportService;

    /**
     * 注入用户服务依赖
     */
    private final UserService userService;

    /**
     * 注入密码服务依赖
     */
    private final PasswordService passwordService;

    /**
     * 注入选题服务依赖
     */
    private final TopicService topicService;

    /**
     * 注入系部服务依赖
     */
    private final DeptService deptService;

    /**
     * 注入专业服务依赖
     */
    private final ProjectService projectService;

    /**
     * 注入学生选题关联服务依赖
     */
    private final StudentTopicSelectionService studentTopicSelectionService;

    /**
     * 构造文件批量导入与统计导出应用服务实现类实例
     *
     * @param sqlExportService             SQL 导出服务
     * @param userService                  用户服务
     * @param passwordService              密码服务
     * @param topicService                 选题服务
     * @param deptService                  系部服务
     * @param projectService               专业服务
     * @param studentTopicSelectionService 学生选题关联服务
     */
    public FileApplicationServiceImpl(
            SqlExportService sqlExportService,
            UserService userService,
            PasswordService passwordService,
            TopicService topicService,
            DeptService deptService,
            ProjectService projectService,
            StudentTopicSelectionService studentTopicSelectionService
    ) {
        this.sqlExportService = sqlExportService;
        this.userService = userService;
        this.passwordService = passwordService;
        this.topicService = topicService;
        this.deptService = deptService;
        this.projectService = projectService;
        this.studentTopicSelectionService = studentTopicSelectionService;
    }

    /// 文件批量导入服务实现 ///

    /**
     * 校验上传 CSV 文件参数并在 Spring 声明式事务中逐行解析、校验系部专业存在性后批量落库用户账号
     *
     * @param multipartFile 上传的 CSV 模板文件
     * @param request       文件上传请求参数
     * @return 批量导入结果响应
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<String> uploadFile(MultipartFile multipartFile, UploadFileRequest request) {
        // 检查参数
        ThrowUtils.throwIf(request == null, CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
        assert request != null;

        ThrowUtils.throwIf(multipartFile == null || multipartFile.isEmpty(), CodeBindMessageEnums.PARAMS_ERROR, "不能上传空的文件");
        assert multipartFile != null;

        Integer userRole = request.getStatus();
        ThrowUtils.throwIf(
                !Objects.equals(UserRoleEnum.STUDENT.getCode(), userRole) && !Objects.equals(UserRoleEnum.TEACHER.getCode(), userRole),
                CodeBindMessageEnums.PARAMS_ERROR,
                "批量导入仅支持学生或教师账号"
        );

        String filename = multipartFile.getOriginalFilename();
        ThrowUtils.throwIf(filename == null || !filename.toLowerCase().endsWith(".csv"), CodeBindMessageEnums.PARAMS_ERROR, "只允许上传 CSV 文件");

        AtomicLong i = new AtomicLong();

        // 批量添加用户账号
        Set<String> temporaryPasswords = new HashSet<>();
        try (
                Reader reader = new InputStreamReader(multipartFile.getInputStream(), StandardCharsets.UTF_8);
                CSVParser csvParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)
        ) {
            for (CSVRecord record : csvParser) {
                i.getAndIncrement();

                boolean teacherImport = Objects.equals(UserRoleEnum.TEACHER.getCode(), userRole);
                int expectedColumnCount = teacherImport ? 6 : 5;
                ThrowUtils.throwIf(
                        record.size() != expectedColumnCount,
                        CodeBindMessageEnums.PARAMS_ERROR,
                        "表中第 " + i + " 行应有 " + expectedColumnCount + " 列，最后一列必须是临时密码"
                );

                // 获取表格数据
                String userAccount = record.get(0).trim();
                String name = record.get(1).trim();
                String department = record.get(2).trim();
                String project = record.get(3).trim();
                String topicAmount = "";
                if (teacherImport) {
                    topicAmount = record.get(4).trim();
                    ThrowUtils.throwIf(StringUtils.isBlank(topicAmount), CodeBindMessageEnums.PARAMS_ERROR, "表中第 " + i + " 行没有填写该教师的最大出题数量");
                    ThrowUtils.throwIf(!StringUtils.isNumeric(topicAmount), CodeBindMessageEnums.PARAMS_ERROR, "表中第 " + i + " 行的最大出题数量必须是正整数");
                }
                String temporaryPassword = record.get(expectedColumnCount - 1).trim();

                // 检查表格空白填写的问题
                ThrowUtils.throwIf(StringUtils.isAnyBlank(userAccount, name, department, temporaryPassword), CodeBindMessageEnums.PARAMS_ERROR, "表中第 " + i + " 行存在必填项空白");
                ThrowUtils.throwIf(userAccount.length() > UserConstant.MAX_USER_ACCOUNT_LENGTH, CodeBindMessageEnums.PARAMS_ERROR, "表中第 " + i + " 行的用户账号不能超过 128 个字符");
                ThrowUtils.throwIf(temporaryPassword.length() < 12 || !passwordService.isPasswordValid(temporaryPassword), CodeBindMessageEnums.PARAMS_ERROR, "表中第 " + i + " 行的临时密码必须至少 12 个字符，且不能超过 72 个 UTF-8 字节");
                ThrowUtils.throwIf(!temporaryPasswords.add(temporaryPassword), CodeBindMessageEnums.PARAMS_ERROR, "表中第 " + i + " 行的临时密码与前面的账号重复，请为每个账号设置不同密码");

                // 检查用户账号是否已存在, 存在则跳过
                User user = userService.getOne(new QueryWrapper<User>().eq("userAccount", userAccount));
                if (user != null) {
                    continue;
                }

                // 创建新的用户
                user = new User();
                user.setUserAccount(userAccount);
                user.setUserPassword(passwordService.encodePassword(temporaryPassword));
                user.setUserName(name);
                user.setUserRole(request.getStatus());
                user.setDept(department);
                user.setProject(project);
                user.setTopicAmount(StringUtils.isBlank(topicAmount) ? null : Integer.parseInt(topicAmount));

                // 保存之前先检查用户填写的系部和专业是否存在, 不存在直接抛出异常回滚
                if (StringUtils.isNotBlank(department)) {
                    ThrowUtils.throwIf(deptService.getOne(new QueryWrapper<Dept>().eq("deptName", department)) == null, CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "表中第 " + i + "行, 导入用户 " + "[" + userAccount + ", " + name + "] " + "时, 系部 [" + department + "] 在系统中不存在, 请添加该系部或修改表格");
                }
                if (StringUtils.isNotBlank(project)) {
                    ThrowUtils.throwIf(projectService.getOne(new QueryWrapper<Project>().eq("projectName", project)) == null, CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, "表中第 " + i + "行, 导入用户 " + "[" + userAccount + ", " + name + "]" + "时, 专业 [" + project + "] 在系统中不存在, 请添加该专业或修改表格");
                }

                // 保存用户
                userService.saveOrUpdate(user);
            }
        } catch (BusinessException e) {
            ThrowUtils.throwIf(true, CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR, e.getExceptionMessage());
        } catch (Exception e) {
            ThrowUtils.throwIf(true, CodeBindMessageEnums.OPERATION_ERROR, "批量添加失败，请检查 CSV 格式");
        }

        return TheResult.success(CodeBindMessageEnums.SUCCESS, "批量添加成功");
    }

    /**
     * 返回课题批量导入暂未开放提示响应
     *
     * @param multipartFile 上传的 CSV 模板文件
     * @return 批量导入结果响应
     */
    @Override
    public BaseResponse<String> uploadFileTopic(MultipartFile multipartFile) {
        return TheResult.notyet("该功能有缺陷暂时不开放使用！");
    }

    /// 统计报表导出数据查询服务实现 ///

    /**
     * 解析当前登录用户的系部权限范围并关联查询已选题学生的用户与题目信息组装 CSV 行列表
     *
     * @return 已选题学生 CSV 行数据列表
     */
    @Override
    public List<List<String>> listSelectedStudentTopicCsvRows() {
        User loginUser = userService.userGetCurrentLoginUser();
        String dept = resolveDepartmentScope(loginUser);

        List<StudentTopicSelection> selectedList = studentTopicSelectionService.list(
                new QueryWrapper<StudentTopicSelection>()
                        .eq("status", StudentTopicSelectionStatusEnum.EN_SELECT.getCode())
        );
        List<List<String>> rows = new ArrayList<>();

        // 获取已选题学生的用户信息和选题信息
        for (StudentTopicSelection studentTopicSelection : selectedList) {
            String userAccount = studentTopicSelection.getUserAccount();
            User user = userService.getOne(new QueryWrapper<User>()
                    .eq("userAccount", userAccount)
                    .eq(dept != null, "dept", dept));
            Topic topic = topicService.getById(studentTopicSelection.getTopicId());
            if (user != null && topic != null) {
                rows.add(Arrays.asList(
                        user.getUserAccount(), user.getUserName(), user.getProject(), user.getDept(),
                        topic.getTopic(), topic.getTeacherName()
                ));
            }
        }
        return rows;
    }

    /**
     * 解析当前登录用户的系部权限范围并筛选出尚未在生效选题记录中的学生实体列表
     *
     * @return 未选题学生实体列表
     */
    @Override
    public List<User> listUnselectedStudentCsvUsers() {
        User loginUser = userService.userGetCurrentLoginUser();
        String dept = resolveDepartmentScope(loginUser);

        List<User> userList = userService.list(
                new QueryWrapper<User>()
                        .eq("userRole", UserRoleEnum.STUDENT.getCode())
                        .eq(dept != null, "dept", dept)
        );
        List<StudentTopicSelection> selectedList = studentTopicSelectionService.list(
                new QueryWrapper<StudentTopicSelection>()
                        .eq("status", StudentTopicSelectionStatusEnum.EN_SELECT.getCode())
        );
        Set<String> selectedUserAccounts = selectedList.stream()
                .map(StudentTopicSelection::getUserAccount)
                .collect(Collectors.toSet());
        return userList.stream()
                .filter(user -> !selectedUserAccounts.contains(user.getUserAccount()))
                .collect(Collectors.toList());
    }

    /**
     * 通过 SqlExportService 执行系统内所有账号查询 SQL 并返回字段映射列表
     *
     * @return 所有账号导出字段映射列表
     */
    @Override
    public List<Map<String, Object>> exportUserListRows() {
        String sql = "SELECT\n" +
                "    `userAccount` AS 帐号,\n" +
                "    `userName` AS 姓名,\n" +
                "    CASE `userRole`\n" +
                "        WHEN 3 THEN '管理员'\n" +
                "        WHEN 2 THEN '专业负责人'\n" +
                "        WHEN 1 THEN '教师'\n" +
                "        WHEN 0 THEN '学生'\n" +
                "        END AS 角色,\n" +
                "    `dept` AS 系部,\n" +
                "    `project` AS 专业,\n" +
                "    `email` AS 邮箱,\n" +
                "    `topicAmount` AS 出题数量或预选数量,\n" +
                "    `status` AS 状态\n" +
                "    FROM `user` WHERE `isDelete` = 0;";
        return sqlExportService.executeQuery(sql);
    }

    /**
     * 通过 SqlExportService 执行系统内所有题目查询 SQL 并返回字段映射列表
     *
     * @return 所有题目导出字段映射列表
     */
    @Override
    public List<Map<String, Object>> exportTopicListRows() {
        String sql = "SELECT\n" +
                "    `teacherName` AS 教师名称,\n" +
                "    `topic` AS 题目,\n" +
                "    `type` AS 题目类型,\n" +
                "    `description` AS 描述,\n" +
                "    `requirement` AS 要求,\n" +
                "    `deptName` AS 系部,\n" +
                "    `deptTeacher` AS 专业负责人,\n" +
                "    `createTime` AS 创建时间,\n" +
                "    `updateTime` AS 更新时间,\n" +
                "    CASE `status`\n" +
                "        WHEN -2 THEN '被打回'\n" +
                "        WHEN -1 THEN '待审核'\n" +
                "        WHEN 0 THEN '没发布'\n" +
                "        WHEN 1 THEN '已发布'\n" +
                "        END AS 状态,\n" +
                "    `reason` AS 打回理由\n" +
                "    FROM `topic` WHERE `isDelete` = 0;";
        return sqlExportService.executeQuery(sql);
    }

    /**
     * 通过 SqlExportService 执行系统内剩余可选题目查询 SQL 并返回字段映射列表
     *
     * @return 剩余题目导出字段映射列表
     */
    @Override
    public List<Map<String, Object>> exportSurplusTopicListRows() {
        String sql = "SELECT t.topic AS 题目名称, t.teacherName AS 指导老师, t.deptName AS 系部名称\n" +
                "FROM topic t\n" +
                "WHERE t.id NOT IN (\n" +
                "    SELECT topicId\n" +
                "    FROM student_topic_selection\n" +
                "    WHERE status = 2\n" +
                "    AND isDelete = 0\n" +
                ")\n" +
                "  AND t.status = 1\n" +
                "  AND t.isDelete = 0;\n";
        return sqlExportService.executeQuery(sql);
    }

    /**
     * 通过 SqlExportService 执行系统内已选学生详细选题查询 SQL 并返回字段映射列表
     *
     * @return 已选学生详细导出字段映射列表
     */
    @Override
    public List<Map<String, Object>> exportSelectedStudentRows() {
        String sql = "SELECT\n" +
                "    u.`userAccount` AS `学号`,\n" +
                "    u.`userName` AS `姓名`,\n" +
                "    u.`dept` AS `系部`,\n" +
                "    u.`project` AS `专业`,\n" +
                "    u.`email` AS `邮箱`,\n" +
                "    s.`updateTime` AS `选择时间`,\n" +
                "    t.`teacherName` AS `指导教师`,\n" +
                "    t.`topic`       AS `题目标题`,\n" +
                "    t.`description` AS `题目内容`,\n" +
                "    t.`requirement` AS `题目要求`\n" +
                "FROM `student_topic_selection` s\n" +
                "JOIN `user` u ON s.userAccount = u.userAccount\n" +
                "JOIN `topic` t ON s.topicId = t.id\n" +
                "WHERE\n" +
                "    s.status = 2 AND\n" +
                "    u.userRole = 0 AND\n" +
                "    s.isDelete = 0 AND\n" +
                "    u.isDelete = 0 AND\n" +
                "    t.isDelete = 0;\n";
        return sqlExportService.executeQuery(sql);
    }

    /**
     * 通过 SqlExportService 执行系统内未选学生详细查询 SQL 并返回字段映射列表
     *
     * @return 未选学生详细导出字段映射列表
     */
    @Override
    public List<Map<String, Object>> exportUnselectedStudentRows() {
        String sql = "SELECT\n" +
                "    u.`userAccount` AS `学号`,\n" +
                "    u.`userName`    AS `姓名`,\n" +
                "    u.`dept`        AS `系部`,\n" +
                "    u.`project`     AS `专业`,\n" +
                "    u.`email`       AS `邮箱`\n" +
                "FROM `user` u\n" +
                "WHERE\n" +
                "    u.userRole = 0 AND\n" +
                "    u.isDelete = 0 AND\n" +
                "NOT EXISTS (\n" +
                "    SELECT 1\n" +
                "    FROM student_topic_selection s\n" +
                "    WHERE\n" +
                "        s.userAccount = u.userAccount AND\n" +
                "        s.status = 2 AND\n" +
                "        s.isDelete = 0\n" +
                ");\n";
        return sqlExportService.executeQuery(sql);
    }

    /// 私有辅助方法 ///

    /**
     * 解析当前登录用户允许导出的系部数据范围
     *
     * @param user 当前登录用户
     * @return 系部名称（管理员返回 null 表示不限制系部范围）
     */
    private String resolveDepartmentScope(User user) {
        if (userService.userIsAdmin(user)) {
            return null;
        }
        ThrowUtils.throwIf(!userService.userIsDept(user), CodeBindMessageEnums.NO_AUTH_ERROR, "当前账号无权导出该数据");
        String dept = user == null ? null : StringUtils.trim(user.getDept());
        ThrowUtils.throwIf(StringUtils.isBlank(dept), CodeBindMessageEnums.NO_AUTH_ERROR, "当前专业负责人账号未配置所属系部");
        return dept;
    }

}
