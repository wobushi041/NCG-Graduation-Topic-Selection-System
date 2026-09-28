package cn.edu.nfu.topicselection.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 管理员重置密码结果
 *
 * @author wobushi041
 */
@Data
@AllArgsConstructor
public class AdminResetPasswordVO {

    /**
     * 用户账号
     */
    private String account;

    /**
     * 随机临时密码
     */
    private String temporaryPassword;

}
