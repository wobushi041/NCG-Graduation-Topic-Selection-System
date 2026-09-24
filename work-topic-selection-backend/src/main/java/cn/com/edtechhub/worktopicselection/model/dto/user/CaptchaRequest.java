package cn.com.edtechhub.worktopicselection.model.dto.user;

import lombok.Data;

/**
 * 发送验证码请求
 *
 * @author wobushi041
 */
@Data
public class CaptchaRequest {

    /**
     * 请求邮箱
     */
    private String email;

}
