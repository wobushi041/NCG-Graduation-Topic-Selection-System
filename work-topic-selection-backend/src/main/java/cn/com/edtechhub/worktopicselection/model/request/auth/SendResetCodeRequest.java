package cn.com.edtechhub.worktopicselection.model.request.auth;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 发送密码重置码请求
 *
 * @author wobushi041
 */
@Data
public class SendResetCodeRequest {

    /**
     * 用户账号
     */
    @NotBlank(message = "账号不能为空")
    @Size(max = 128, message = "账号不能超过 128 个字符")
    private String account;

}
