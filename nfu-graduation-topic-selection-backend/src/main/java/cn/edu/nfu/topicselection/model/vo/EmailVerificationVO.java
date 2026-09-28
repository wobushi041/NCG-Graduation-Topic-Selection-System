package cn.edu.nfu.topicselection.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 邮箱验证码校验结果
 *
 * @author wobushi041
 */
@Data
@AllArgsConstructor
public class EmailVerificationVO {

    /**
     * 一次性邮箱验证凭证
     */
    private String proofToken;

    /**
     * 凭证剩余有效秒数
     */
    private long expiresInSeconds;

}
