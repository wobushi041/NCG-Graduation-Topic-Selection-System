package cn.com.edtechhub.worktopicselection.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminResetPasswordVO {
    private String account;
    private String temporaryPassword;
}
