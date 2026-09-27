package cn.com.edtechhub.worktopicselection.model.dto.auth;

import cn.com.edtechhub.worktopicselection.validation.Utf8ByteLength;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class ResetPasswordByCodeRequest {

    @NotBlank(message = "账号不能为空")
    @Size(max = 128, message = "账号不能超过 128 个字符")
    private String account;

    @NotBlank(message = "重置码不能为空")
    @Size(min = 12, max = 12, message = "重置码格式不正确")
    private String resetCode;

    @NotBlank(message = "新密码不能为空")
    @Utf8ByteLength(min = 8, max = 72, message = "新密码长度必须为 8 到 72 个 UTF-8 字节")
    private String newPassword;
}
