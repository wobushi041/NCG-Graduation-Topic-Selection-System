package cn.edu.nfu.topicselection.manager.sentinel;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 认证接口 Sentinel QPS 配置
 *
 * @author wobushi041
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.sentinel.auth")
public class SentinelRateLimitProperties {

    /**
     * 登录接口 QPS 阈值
     */
    private double login = 100;

    /**
     * 退出登录接口 QPS 阈值
     */
    private double logout = 300;

    /**
     * 角色切换接口 QPS 阈值
     */
    private double roleSwitch = 100;

    /**
     * 角色切换可用性接口 QPS 阈值
     */
    private double roleSwitchAvailability = 300;

    /**
     * 管理员重置密码接口 QPS 阈值
     */
    private double passwordAdminReset = 20;

    /**
     * 修改密码接口 QPS 阈值
     */
    private double passwordChange = 50;

    /**
     * 重置密码接口 QPS 阈值
     */
    private double passwordReset = 50;

    /**
     * 发送密码重置码接口 QPS 阈值
     */
    private double passwordResetCodeSend = 20;

    /**
     * 发送邮箱验证码接口 QPS 阈值
     */
    private double emailCodeSend = 20;

    /**
     * 校验邮箱验证码接口 QPS 阈值
     */
    private double emailCodeVerify = 100;

    /**
     * 学生预选或取消预选课题接口 QPS 阈值
     */
    private double topicSelectionPreselect = 200;

    /**
     * 学生确认最终选题接口 QPS 阈值
     */
    private double topicSelectionConfirm = 100;

    /**
     * 教师确认学生选题接口 QPS 阈值
     */
    private double topicSelectionAssignStudent = 100;

    /**
     * 退选课题接口 QPS 阈值
     */
    private double topicSelectionWithdraw = 100;

    /**
     * 教师查询已选本人课题学生列表接口 QPS 阈值
     */
    private double topicSelectionQuerySelectedStudents = 300;

    /**
     * 学生查询预选课题列表接口 QPS 阈值
     */
    private double topicSelectionQueryPreselected = 300;

    /**
     * 学生查询最终选题列表接口 QPS 阈值
     */
    private double topicSelectionQuerySelected = 300;

    /**
     * 学生查询最终选题确认时间接口 QPS 阈值
     */
    private double topicSelectionQueryChoiceTime = 300;

    /**
     * 教师按课题 ID 查询学生列表接口 QPS 阈值
     */
    private double topicSelectionQueryStudentsByTopic = 300;

    /**
     * 教师添加课题接口 QPS 阈值
     */
    private double topicAdd = 100;

    /**
     * 教师删除课题接口 QPS 阈值
     */
    private double topicDelete = 100;

    /**
     * 管理员查询教师题目上限接口 QPS 阈值
     */
    private double topicQuotaGet = 200;

    /**
     * 管理员设置教师题目上限接口 QPS 阈值
     */
    private double topicQuotaSet = 100;

    /**
     * 审核或重新提交课题接口 QPS 阈值
     */
    private double topicReviewCheck = 100;

    /**
     * 批量发布课题开放时间接口 QPS 阈值
     */
    private double topicPublicationPublish = 100;

    /**
     * 批量取消课题开放时间接口 QPS 阈值
     */
    private double topicPublicationUnpublish = 100;

    /**
     * 教师修改课题接口 QPS 阈值
     */
    private double topicUpdate = 100;

    /**
     * 课题 AI 审核等级检测接口 QPS 阈值（高成本外部资源）
     */
    private double topicReviewAiLevel = 20;

    /**
     * 添加学院接口 QPS 阈值
     */
    private double organizationCollegeAdd = 50;

    /**
     * 添加专业接口 QPS 阈值
     */
    private double organizationMajorAdd = 50;

    /**
     * 更新专业所属选题组接口 QPS 阈值
     */
    private double organizationMajorUpdateGroup = 50;

    /**
     * 删除学院接口 QPS 阈值
     */
    private double organizationCollegeDelete = 50;

    /**
     * 删除专业接口 QPS 阈值
     */
    private double organizationMajorDelete = 50;

    /**
     * 分页查询学院接口 QPS 阈值
     */
    private double organizationCollegeQueryPage = 200;

    /**
     * 查询学院下拉列表接口 QPS 阈值
     */
    private double organizationCollegeQueryList = 300;

    /**
     * 分页查询专业接口 QPS 阈值
     */
    private double organizationMajorQueryPage = 200;

    /**
     * 查询专业下拉列表接口 QPS 阈值
     */
    private double organizationMajorQueryList = 300;

    /**
     * 查询当前登录教师选题组列表接口 QPS 阈值
     */
    private double teacherGroupQuerySelf = 200;

    /**
     * 批量查询教师选题组及额度接口 QPS 阈值
     */
    private double teacherGroupQueryBatch = 200;

    /**
     * 查询系统全部选题组名称列表接口 QPS 阈值
     */
    private double teacherGroupQueryAll = 300;

    /// 系统开关、配置与系统诊断接口流控配置 ///

    /**
     * 查询跨学院选题开关接口 QPS 阈值
     */
    private double policyCrossTopicQuery = 120;

    /**
     * 设置跨学院选题开关接口 QPS 阈值
     */
    private double policyCrossTopicUpdate = 30;

    /**
     * 查询学生查看课题开关接口 QPS 阈值
     */
    private double policyViewTopicQuery = 120;

    /**
     * 设置学生查看课题开关接口 QPS 阈值
     */
    private double policyViewTopicUpdate = 30;

    /**
     * 查询单选模式开关接口 QPS 阈值
     */
    private double policySingleChoiceQuery = 120;

    /**
     * 设置单选模式开关接口 QPS 阈值
     */
    private double policySingleChoiceUpdate = 30;

    /**
     * 查询退选加锁状态接口 QPS 阈值
     */
    private double policyTopicLockQuery = 200;

    /**
     * 设置退选加锁状态及时间接口 QPS 阈值
     */
    private double policyTopicLockUpdate = 30;

    /**
     * 查询学院跨选配置接口 QPS 阈值
     */
    private double policyCollegeConfigQuery = 60;

    /**
     * 设置学院跨选配置接口 QPS 阈值
     */
    private double policyCollegeConfigUpdate = 30;

    /**
     * 清除学院跨选配置接口 QPS 阈值
     */
    private double policyCollegeConfigDelete = 30;

    /**
     * 系统连通性测试诊断接口 QPS 阈值
     */
    private double systemDiagnosticsTest = 200;

    /**
     * 查询系统信息面板接口 QPS 阈值
     */
    private double systemInfoQuery = 60;

    /// 用户管理接口流控配置 ///

    /**
     * 创建用户接口 QPS 阈值
     */
    private double userManageAdd = 30;

    /**
     * 删除用户接口 QPS 阈值
     */
    private double userManageDelete = 30;

    /**
     * 更新用户接口 QPS 阈值
     */
    private double userManageUpdate = 60;

    /**
     * 获取当前登录用户接口 QPS 阈值
     */
    private double userQueryCurrent = 300;

    /**
     * 分页查询用户接口 QPS 阈值
     */
    private double userQueryPage = 120;

    /**
     * 查询教师脱敏列表接口 QPS 阈值
     */
    private double userQueryTeacherList = 120;

    /**
     * 根据 id 查询用户实体接口 QPS 阈值
     */
    private double userQueryById = 120;

    /**
     * 根据 id 查询用户脱敏视图接口 QPS 阈值
     */
    private double userQueryVoById = 120;

    /// 查询与统计域接口流控配置 ///

    /**
     * 按角色分页查询可见课题接口 QPS 阈值
     */
    private double queryTopicPage = 200;

    /**
     * 查询选题统计总览接口 QPS 阈值
     */
    private double querySelectionSituation = 120;

    /**
     * 分页查询学院教师接口 QPS 阈值
     */
    private double queryCollegeTeacher = 150;

    /**
     * 查询本系未选题学生列表接口 QPS 阈值
     */
    private double querySelectionUnselectedStudents = 120;

    /**
     * 管理员分页查询课题接口 QPS 阈值
     */
    private double queryTopicAdminPage = 150;

    /**
     * 分页查询用户脱敏视图列表接口 QPS 阈值
     */
    private double queryUserVoPage = 120;

    /**
     * 查询用户姓名列表接口 QPS 阈值
     */
    private double queryUserNameList = 150;

    /**
     * 查询待审核课题相关学院教师接口 QPS 阈值
     */
    private double queryCollegePendingTeacher = 120;

    /// 文件导入导出与 AI 域接口流控配置 ///

    /**
     * 批量导入用户接口 QPS 阈值
     */
    private double fileUserImport = 10;

    /**
     * 批量导入课题接口 QPS 阈值
     */
    private double fileTopicImport = 10;

    /**
     * 导出已选题学生课题列表接口 QPS 阈值
     */
    private double fileSelectionSelectedExport = 20;

    /**
     * 导出未选题学生列表接口 QPS 阈值
     */
    private double fileSelectionUnselectedExport = 20;

    /**
     * 导出系统内所有账号接口 QPS 阈值
     */
    private double fileExportUserList = 20;

    /**
     * 导出系统内所有题目接口 QPS 阈值
     */
    private double fileExportTopicList = 20;

    /**
     * 导出系统内剩余题目接口 QPS 阈值
     */
    private double fileExportSurplusTopicList = 20;

    /**
     * 导出系统内已选学生详情接口 QPS 阈值
     */
    private double fileExportStudentEnSelect = 20;

    /**
     * 导出系统内未选学生详情接口 QPS 阈值
     */
    private double fileExportStudentUnSelect = 20;

    /**
     * AI 问答发送消息接口 QPS 阈值
     */
    private double aiChatSend = 30;

}
