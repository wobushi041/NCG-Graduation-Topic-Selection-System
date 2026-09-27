package cn.com.edtechhub.worktopicselection.model.dto.auth;

import cn.com.edtechhub.worktopicselection.validation.Utf8ByteLength;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class LoginRequest {

    @NotBlank(message = "账号不能为空")
    @Size(max = 128, message = "账号不能超过 128 个字符")
    private String account;

    @NotBlank(message = "密码不能为空")
    @Utf8ByteLength(max = 72, message = "密码不能超过 72 个 UTF-8 字节")
    private String password;
}
