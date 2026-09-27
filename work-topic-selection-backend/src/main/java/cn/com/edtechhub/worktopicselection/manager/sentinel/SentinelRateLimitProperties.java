package cn.com.edtechhub.worktopicselection.manager.sentinel;

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

}

