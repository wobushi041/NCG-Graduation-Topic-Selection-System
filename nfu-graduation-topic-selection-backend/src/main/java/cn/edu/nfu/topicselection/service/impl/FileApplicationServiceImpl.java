package cn.edu.nfu.topicselection.service.impl;

import cn.edu.nfu.topicselection.constant.UserConstant;
import cn.edu.nfu.topicselection.exception.BusinessException;
import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import cn.edu.nfu.topicselection.model.entity.College;
import cn.edu.nfu.topicselection.model.entity.Major;
import cn.edu.nfu.topicselection.model.entity.StudentTopicSelection;
import cn.edu.nfu.topicselection.model.entity.Topic;
import cn.edu.nfu.topicselection.model.entity.User;
import cn.edu.nfu.topicselection.model.enums.StudentTopicSelectionStatusEnum;
import cn.edu.nfu.topicselection.model.enums.UserRoleEnum;
import cn.edu.nfu.topicselection.model.request.file.UploadFileRequest;
import cn.edu.nfu.topicselection.response.BaseResponse;
import cn.edu.nfu.topicselection.response.TheResult;
import cn.edu.nfu.topicselection.service.CollegeService;
import cn.edu.nfu.topicselection.service.FileApplicationService;
import cn.edu.nfu.topicselection.service.PasswordService;
import cn.edu.nfu.topicselection.service.MajorService;
import cn.edu.nfu.topicselection.service.SqlExportService;
import cn.edu.nfu.topicselection.service.StudentTopicSelectionService;
import cn.edu.nfu.topicselection.service.TopicService;
import cn.edu.nfu.topicselection.service.UserService;
import cn.edu.nfu.topicselection.utils.ThrowUtils;
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
     * 注入学院服务依赖
     */
    private final CollegeService collegeService;

    /**
     * 注入专业服务依赖
     */
    private final MajorService majorService;

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
     * @param collegeService                  学院服务
     * @param majorService               专业服务
     * @param studentTopicSelectionService 学生选题关联服务
     */
    public FileApplicationServiceImpl(
            SqlExportService sqlExportService,
            UserService userService,
            PasswordService passwordService,
            TopicService topicService,
            CollegeService collegeService,
            MajorService majorService,
            StudentTopicSelectionService studentTopicSelectionService
    ) {
        this.sqlExportService = sqlExportService;
        this.userService = userService;
        this.passwordService = passwordService;
        this.topicService = topicService;
        this.collegeService = collegeService;
        this.majorService = majorService;
        this.studentTopicSelectionService = studentTopicSelectionService;
    }

    /// 文件批量导入服务实现 ///

    /**
     * 校验上传 CSV 文件参数并在 Spring 声明式事务中逐行解析、校验学院专业存在性后批量落库用户账号
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
                String major = record.get(3).trim();
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
                user.setTopicAmount(StringUtils.isBlank(topicAmount) ? null : Integer.parseInt(topicAmount));

                // 保存之前解析学院和专业名称并校验归属关系
                College college = collegeService.getOne(new QueryWrapper<College>().eq("collegeName", department));
                ThrowUtils.throwIf(college == null, CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR,
                        "表中第 " + i + "行, 导入用户 [" + userAccount + ", " + name + "] 时, 学院 [" + department + "] 在系统中不存在");
                assert college != null;
                user.setCollegeId(college.getId());
                if (StringUtils.isNotBlank(major)) {
                    Major majorEntity = majorService.getOne(new QueryWrapper<Major>()
                            .eq("majorName", major)
                            .eq("collegeId", college.getId()));
                    ThrowUtils.throwIf(majorEntity == null, CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR,
                            "表中第 " + i + "行, 导入用户 [" + userAccount + ", " + name + "] 时, 专业 [" + major + "] 不属于所选学院");
                    assert majorEntity != null;
                    user.setMajorId(majorEntity.getId());
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
     * 解析当前登录用户的学院权限范围并关联查询已选题学生的用户与题目信息组装 CSV 行列表
     *
     * @return 已选题学生 CSV 行数据列表
     */
    @Override
    public List<List<String>> listSelectedStudentTopicCsvRows() {
        User loginUser = userService.userGetCurrentLoginUser();
        Long collegeId = resolveCollegeScope(loginUser);

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
                    .eq(collegeId != null, "collegeId", collegeId));
            Topic topic = topicService.getById(studentTopicSelection.getTopicId());
            if (user != null && topic != null) {
                rows.add(Arrays.asList(
                        user.getUserAccount(), user.getUserName(), resolveMajorName(user.getMajorId()),
                        resolveCollegeName(user.getCollegeId()),
                        topic.getTopic(), topic.getTeacherName()
                ));
            }
        }
        return rows;
    }

    /**
     * 解析当前登录用户的学院权限范围并筛选出尚未在生效选题记录中的学生实体列表
     *
     * @return 未选题学生实体列表
     */
    @Override
    public List<User> listUnselectedStudentCsvUsers() {
        User loginUser = userService.userGetCurrentLoginUser();
        Long collegeId = resolveCollegeScope(loginUser);

        List<User> userList = userService.list(
                new QueryWrapper<User>()
                        .eq("userRole", UserRoleEnum.STUDENT.getCode())
                        .eq(collegeId != null, "collegeId", collegeId)
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
                "    u.`userAccount` AS 帐号,\n" +
                "    u.`userName` AS 姓名,\n" +
                "    CASE u.`userRole`\n" +
                "        WHEN 3 THEN '管理员'\n" +
                "        WHEN 2 THEN '选题负责人'\n" +
                "        WHEN 1 THEN '教师'\n" +
                "        WHEN 0 THEN '学生'\n" +
                "        END AS 角色,\n" +
                "    c.`collegeName` AS 学院,\n" +
                "    m.`majorName` AS 专业,\n" +
                "    g.`groupName` AS 负责选题组,\n" +
                "    u.`email` AS 邮箱,\n" +
                "    u.`topicAmount` AS 出题数量或预选数量,\n" +
                "    u.`status` AS 状态\n" +
                "    FROM `user` u\n" +
                "    LEFT JOIN `college` c ON u.`collegeId` = c.`id`\n" +
                "    LEFT JOIN `major` m ON u.`majorId` = m.`id`\n" +
                "    LEFT JOIN `topic_group` g ON u.`topicGroupId` = g.`id`\n" +
                "    WHERE u.`isDelete` = 0;";
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
                "    t.`teacherName` AS 教师名称,\n" +
                "    t.`topic` AS 题目,\n" +
                "    t.`type` AS 题目类型,\n" +
                "    t.`description` AS 描述,\n" +
                "    t.`requirement` AS 要求,\n" +
                "    g.`groupName` AS 选题组,\n" +
                "    c.`collegeName` AS 学院,\n" +
                "    t.`createTime` AS 创建时间,\n" +
                "    t.`updateTime` AS 更新时间,\n" +
                "    CASE t.`status`\n" +
                "        WHEN -2 THEN '被打回'\n" +
                "        WHEN -1 THEN '待审核'\n" +
                "        WHEN 0 THEN '没发布'\n" +
                "        WHEN 1 THEN '已发布'\n" +
                "        END AS 状态,\n" +
                "    t.`reason` AS 打回理由\n" +
                "    FROM `topic` t\n" +
                "    JOIN `topic_group` g ON t.`topicGroupId` = g.`id`\n" +
                "    JOIN `college` c ON g.`collegeId` = c.`id`\n" +
                "    WHERE t.`isDelete` = 0;";
        return sqlExportService.executeQuery(sql);
    }

    /**
     * 通过 SqlExportService 执行系统内剩余可选题目查询 SQL 并返回字段映射列表
     *
     * @return 剩余题目导出字段映射列表
     */
    @Override
    public List<Map<String, Object>> exportSurplusTopicListRows() {
        String sql = "SELECT t.topic AS 题目名称, t.teacherName AS 指导老师, c.collegeName AS 学院, g.groupName AS 选题组\n" +
                "FROM topic t\n" +
                "JOIN topic_group g ON t.topicGroupId = g.id\n" +
                "JOIN college c ON g.collegeId = c.id\n" +
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
                "    c.`collegeName` AS `学院`,\n" +
                "    m.`majorName` AS `专业`,\n" +
                "    u.`email` AS `邮箱`,\n" +
                "    s.`updateTime` AS `选择时间`,\n" +
                "    t.`teacherName` AS `指导教师`,\n" +
                "    t.`topic`       AS `题目标题`,\n" +
                "    t.`description` AS `题目内容`,\n" +
                "    t.`requirement` AS `题目要求`\n" +
                "FROM `student_topic_selection` s\n" +
                "JOIN `user` u ON s.userAccount = u.userAccount\n" +
                "JOIN `topic` t ON s.topicId = t.id\n" +
                "LEFT JOIN `college` c ON u.collegeId = c.id\n" +
                "LEFT JOIN `major` m ON u.majorId = m.id\n" +
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
                "    c.`collegeName` AS `学院`,\n" +
                "    m.`majorName` AS `专业`,\n" +
                "    u.`email`       AS `邮箱`\n" +
                "FROM `user` u\n" +
                "LEFT JOIN `college` c ON u.collegeId = c.id\n" +
                "LEFT JOIN `major` m ON u.majorId = m.id\n" +
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
     * 解析当前登录用户允许导出的学院数据范围
     *
     * @param user 当前登录用户
     * @return 学院 id（管理员返回 null 表示不限制学院范围）
     */
    private Long resolveCollegeScope(User user) {
        if (userService.userIsAdmin(user)) {
            return null;
        }
        ThrowUtils.throwIf(!userService.userIsTopicLeader(user), CodeBindMessageEnums.NO_AUTH_ERROR, "当前账号无权导出该数据");
        Long collegeId = user == null ? null : user.getCollegeId();
        ThrowUtils.throwIf(collegeId == null, CodeBindMessageEnums.NO_AUTH_ERROR, "当前选题负责人账号未配置所属学院");
        return collegeId;
    }

    /**
     * 根据学院 id 获取用于导出的学院名称
     *
     * @param collegeId 学院 id
     * @return 学院名称
     */
    private String resolveCollegeName(Long collegeId) {
        College college = collegeId == null ? null : collegeService.getById(collegeId);
        return college == null ? "" : college.getCollegeName();
    }

    /**
     * 根据专业 id 获取用于导出的专业名称
     *
     * @param majorId 专业 id
     * @return 专业名称
     */
    private String resolveMajorName(Long majorId) {
        Major major = majorId == null ? null : majorService.getById(majorId);
        return major == null ? "" : major.getMajorName();
    }

}
