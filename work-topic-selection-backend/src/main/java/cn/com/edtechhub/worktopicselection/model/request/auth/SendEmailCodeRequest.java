package cn.com.edtechhub.worktopicselection.model.request.auth;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 发送邮箱验证码请求
 *
 * @author wobushi041
 */
@Data
public class SendEmailCodeRequest {

    /**
     * 接收验证码的邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 254, message = "邮箱不能超过 254 个字符")
    private String email;

}
