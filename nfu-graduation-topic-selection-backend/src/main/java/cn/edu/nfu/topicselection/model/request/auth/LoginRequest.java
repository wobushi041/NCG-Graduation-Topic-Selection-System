package cn.edu.nfu.topicselection.model.request.auth;

import cn.edu.nfu.topicselection.validation.Utf8ByteLength;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 账号密码登录请求
 *
 * @author wobushi041
 */
@Data
public class LoginRequest {

    /**
     * 用户账号
     */
    @NotBlank(message = "账号不能为空")
    @Size(max = 128, message = "账号不能超过 128 个字符")
    private String account;

    /**
     * 用户密码
     */
    @NotBlank(message = "密码不能为空")
    @Utf8ByteLength(max = 72, message = "密码不能超过 72 个 UTF-8 字节")
    private String password;

}
