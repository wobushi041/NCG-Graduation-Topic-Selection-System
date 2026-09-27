package cn.com.edtechhub.worktopicselection.service;

import cn.com.edtechhub.worktopicselection.model.dto.auth.AdminResetPasswordRequest;
import cn.com.edtechhub.worktopicselection.model.dto.auth.ChangePasswordRequest;
import cn.com.edtechhub.worktopicselection.model.dto.auth.ResetPasswordByCodeRequest;
import cn.com.edtechhub.worktopicselection.model.vo.AdminResetPasswordVO;

public interface PasswordService {
    String encodePassword(String rawPassword);
    String encodePasswordForMigration(String rawPassword);
    boolean matchesPassword(String rawPassword, String encodedPassword);
    boolean needsPasswordUpgrade(String encodedPassword);
    String generateTemporaryPassword();
    boolean isPasswordValid(String rawPassword);
    AdminResetPasswordVO adminReset(AdminResetPasswordRequest request);
    Long changePassword(ChangePasswordRequest request, String clientIp);
    Long resetPassword(ResetPasswordByCodeRequest request);
}
