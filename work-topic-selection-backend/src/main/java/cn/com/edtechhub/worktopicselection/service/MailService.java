package cn.com.edtechhub.worktopicselection.service;

/**
 * 邮箱通知服务接口
 *
 * @author wobushi041
 */
public interface MailService {

    /**
     * 向指定收件人发送系统日志通知邮件
     *
     * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param text    邮件内容
     */
    void sendSystemMail(String to, String subject, String text);

    /**
     * 向指定收件人发送临时密码通知邮件
     *
     * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param text    邮件内容
     */
    void sendCodeMail(String to, String subject, String text);

    /**
     * 向指定收件人发送验证码通知邮件
     *
     * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param text    邮件内容
     */
    void sendCaptchaMail(String to, String subject, String text);

    /**
     * 向指定收件人发送题目审核打回通知邮件
     *
     * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param text    邮件内容
     */
    void sendReasonMail(String to, String subject, String text);

    /**
     * 向指定收件人发送题目退选确认通知邮件
     *
     * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param text    邮件内容
     */
    void sendTopicMail(String to, String subject, String text);

}
