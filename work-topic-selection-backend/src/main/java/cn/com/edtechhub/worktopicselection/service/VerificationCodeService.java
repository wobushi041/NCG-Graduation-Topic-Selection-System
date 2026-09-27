package cn.com.edtechhub.worktopicselection.service;

import cn.com.edtechhub.worktopicselection.model.vo.EmailVerificationVO;

/**
 * 提供邮箱验证码和密码重置码业务能力
 *
 * @author wobushi041
 */
public interface VerificationCodeService {

    /**
     * 向账号绑定邮箱发送密码重置码
     *
     * @param account  账号
     * @param clientIp 客户端 IP
     * @return 通用发送结果
     */
    String sendPasswordResetCode(String account, String clientIp);

    /**
     * 向指定邮箱发送验证码
     *
     * @param email    邮箱
     * @param clientIp 客户端 IP
     * @return 发送结果
     */
    String sendEmailCode(String email, String clientIp);

    /**
     * 校验邮箱验证码并签发一次性凭证
     *
     * @param email 邮箱
     * @param code  验证码
     * @return 邮箱验证凭证
     */
    EmailVerificationVO verifyEmailCode(String email, String code);

    /**
     * 校验并消费邮箱验证凭证
     *
     * @param email      邮箱
     * @param proofToken 邮箱验证凭证
     * @return 凭证是否有效并成功消费
     */
    boolean consumeEmailProof(String email, String proofToken);

    /**
     * 校验并消费账号的密码重置码
     *
     * @param account   账号
     * @param resetCode 密码重置码
     * @return Redis 原子消费结果
     */
    long consumePasswordResetCode(String account, String resetCode);

    /**
     * 规范化邮箱地址
     *
     * @param email 邮箱
     * @return 规范化邮箱地址
     */
    String normalizeEmail(String email);

}
