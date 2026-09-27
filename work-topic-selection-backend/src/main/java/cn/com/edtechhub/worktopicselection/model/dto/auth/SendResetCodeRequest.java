package cn.com.edtechhub.worktopicselection.model.dto.auth;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class SendResetCodeRequest {

    @NotBlank(message = "账号不能为空")
    @Size(max = 128, message = "账号不能超过 128 个字符")
    private String account;
}
