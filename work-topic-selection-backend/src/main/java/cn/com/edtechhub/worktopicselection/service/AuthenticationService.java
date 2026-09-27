package cn.com.edtechhub.worktopicselection.service;

import cn.com.edtechhub.worktopicselection.model.dto.auth.LoginRequest;
import cn.com.edtechhub.worktopicselection.model.dto.auth.RoleSwitchRequest;
import cn.com.edtechhub.worktopicselection.model.vo.LoginUserVO;
import cn.com.edtechhub.worktopicselection.model.vo.RoleSwitchAvailabilityVO;

public interface AuthenticationService {
    LoginUserVO login(LoginRequest request, String clientIp, String device);
    boolean logout();
    LoginUserVO switchRole(RoleSwitchRequest request, String device);
    RoleSwitchAvailabilityVO getRoleSwitchAvailability();
}
