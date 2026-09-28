package cn.edu.nfu.topicselection.model.dto.user;

import lombok.Data;

/**
 * 发送临时请求
 *
 * @author wobushi041
 */
@Data
public class SendCodeRequest {

    /**
     * 用户帐号
     */
    private String userAccount;

}
