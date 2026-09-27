package cn.com.edtechhub.worktopicselection.model.dto.auth;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class AdminResetPasswordRequest {

    @NotBlank(message = "账号不能为空")
    @Size(max = 128, message = "账号不能超过 128 个字符")
    private String account;

    @NotBlank(message = "用户名不能为空")
    private String name;
}
