package cn.edu.nfu.topicselection.model.request.auth;

import cn.edu.nfu.topicselection.validation.Utf8ByteLength;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 使用一次性重置码修改密码请求
 *
 * @author wobushi041
 */
@Data
public class ResetPasswordByCodeRequest {

    /**
     * 用户账号
     */
    @NotBlank(message = "账号不能为空")
    @Size(max = 128, message = "账号不能超过 128 个字符")
    private String account;

    /**
     * 一次性密码重置码
     */
    @NotBlank(message = "重置码不能为空")
    @Size(min = 12, max = 12, message = "重置码格式不正确")
    private String resetCode;

    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    @Utf8ByteLength(min = 8, max = 72, message = "新密码长度必须为 8 到 72 个 UTF-8 字节")
    private String newPassword;

}
