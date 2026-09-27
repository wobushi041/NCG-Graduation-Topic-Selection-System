package cn.com.edtechhub.worktopicselection.model.dto.auth;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class RoleSwitchRequest {

    @NotBlank(message = "目标角色不能为空")
    @Pattern(regexp = "teacher|dept", message = "目标角色只能是 teacher 或 dept")
    private String targetRole;
}
