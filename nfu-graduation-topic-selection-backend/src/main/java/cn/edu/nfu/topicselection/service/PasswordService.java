package cn.edu.nfu.topicselection.service;

import cn.edu.nfu.topicselection.model.request.auth.AdminResetPasswordRequest;
import cn.edu.nfu.topicselection.model.request.auth.ChangePasswordRequest;
import cn.edu.nfu.topicselection.model.request.auth.ResetPasswordByCodeRequest;
import cn.edu.nfu.topicselection.model.vo.AdminResetPasswordVO;

/**
 * 提供密码校验、编码、迁移和重置业务能力
 *
 * @author wobushi041
 */
public interface PasswordService {

    /**
     * 校验密码长度并编码为安全密码摘要
     *
     * @param rawPassword 原始密码
     * @return 密码摘要
     */
    String encodePassword(String rawPassword);

    /**
     * 将符合迁移条件的旧密码编码为安全密码摘要
     *
     * @param rawPassword 原始密码
     * @return 密码摘要
     */
    String encodePasswordForMigration(String rawPassword);

    /**
     * 校验原始密码是否与已编码密码匹配
     *
     * @param rawPassword     原始密码
     * @param encodedPassword 已编码密码
     * @return 密码是否匹配
     */
    boolean matchesPassword(String rawPassword, String encodedPassword);

    /**
     * 判断已编码密码是否需要升级安全格式
     *
     * @param encodedPassword 已编码密码
     * @return 是否需要升级
     */
    boolean needsPasswordUpgrade(String encodedPassword);

    /**
     * 生成符合密码约束的随机临时密码
     *
     * @return 随机临时密码
     */
    String generateTemporaryPassword();

    /**
     * 判断原始密码是否符合字节长度约束
     *
     * @param rawPassword 原始密码
     * @return 密码是否有效
     */
    boolean isPasswordValid(String rawPassword);

    /**
     * 由管理员为指定账号重置临时密码
     *
     * @param request 管理员重置密码请求
     * @return 账号与临时密码
     */
    AdminResetPasswordVO adminReset(AdminResetPasswordRequest request);

    /**
     * 校验当前密码后修改账号密码
     *
     * @param request  修改密码请求
     * @param clientIp 客户端 IP
     * @return 用户 id
     */
    Long changePassword(ChangePasswordRequest request, String clientIp);

    /**
     * 消费一次性重置码并修改账号密码
     *
     * @param request 重置密码请求
     * @return 用户 id
     */
    Long resetPassword(ResetPasswordByCodeRequest request);

}
