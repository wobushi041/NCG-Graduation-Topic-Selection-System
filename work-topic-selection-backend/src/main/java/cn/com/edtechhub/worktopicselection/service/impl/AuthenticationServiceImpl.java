package cn.com.edtechhub.worktopicselection.service.impl;

import cn.com.edtechhub.worktopicselection.exception.CodeBindMessageEnums;
import cn.com.edtechhub.worktopicselection.manager.satoken.AuthSessionManager;
import cn.com.edtechhub.worktopicselection.manager.security.SecurityRateLimitManager;
import cn.com.edtechhub.worktopicselection.model.dto.auth.LoginRequest;
import cn.com.edtechhub.worktopicselection.model.dto.auth.RoleSwitchRequest;
import cn.com.edtechhub.worktopicselection.model.entity.User;
import cn.com.edtechhub.worktopicselection.model.enums.UserRoleEnum;
import cn.com.edtechhub.worktopicselection.model.vo.LoginUserVO;
import cn.com.edtechhub.worktopicselection.model.vo.RoleSwitchAvailabilityVO;
import cn.com.edtechhub.worktopicselection.service.AuthenticationService;
import cn.com.edtechhub.worktopicselection.service.PasswordService;
import cn.com.edtechhub.worktopicselection.service.UserService;
import cn.com.edtechhub.worktopicselection.utils.ThrowUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final String DUMMY_PASSWORD_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final UserService userService;
    private final PasswordService passwordService;
    private final SecurityRateLimitManager rateLimitManager;
    private final AuthSessionManager authSessionManager;

    public AuthenticationServiceImpl(UserService userService, PasswordService passwordService,
                                     SecurityRateLimitManager rateLimitManager,
                                     AuthSessionManager authSessionManager) {
        this.userService = userService;
        this.passwordService = passwordService;
        this.rateLimitManager = rateLimitManager;
        this.authSessionManager = authSessionManager;
    }

    @Override
    public LoginUserVO login(LoginRequest request, String clientIp, String device) {
        String account = request.getAccount().trim();
        User user = userService.userIsExist(account);
        String rateLimitAccount = SecurityRateLimitManager.canonicalAccount(account, user);
        rateLimitManager.enforceLogin(rateLimitAccount, clientIp);
        boolean matches = user == null
                ? passwordService.matchesPassword(request.getPassword(), DUMMY_PASSWORD_HASH) && false
                : passwordService.matchesPassword(request.getPassword(), user.getUserPassword());
        ThrowUtils.throwIf(!matches, CodeBindMessageEnums.PARAMS_ERROR, "账号或密码错误");

        UserRoleEnum role = UserRoleEnum.getEnums(user.getUserRole());
        ThrowUtils.throwIf(role == null || role == UserRoleEnum.BAN_ROLE,
                CodeBindMessageEnums.NO_AUTH_ERROR, "账号已被禁用，请联系系统管理员");
        ThrowUtils.throwIf(StringUtils.isBlank(user.getStatus()), CodeBindMessageEnums.USER_INIT_PASSWD,
                "您正在使用初始临时密码, 请修改密码后再登陆");

        if (passwordService.needsPasswordUpgrade(user.getUserPassword())) {
            String oldHash = user.getUserPassword();
            String upgradedHash = passwordService.encodePasswordForMigration(request.getPassword());
            boolean upgraded = userService.update(new UpdateWrapper<User>()
                    .eq("id", user.getId())
                    .eq("userPassword", oldHash)
                    .set("userPassword", upgradedHash));
            ThrowUtils.throwIf(!upgraded, CodeBindMessageEnums.OPERATION_ERROR, "升级密码安全格式失败，请稍后重试");
            user.setUserPassword(upgradedHash);
        }

        authSessionManager.login(user, device);
        rateLimitManager.releaseLogin(rateLimitAccount, clientIp);
        return userService.getLoginUserVO(user);
    }

    @Override
    public boolean logout() {
        authSessionManager.logoutCurrent();
        return true;
    }

    @Override
    public LoginUserVO switchRole(RoleSwitchRequest request, String device) {
        User loginUser = userService.userGetCurrentLoginUser();
        UserRoleEnum targetRole = roleByDescription(request.getTargetRole());
        ThrowUtils.throwIf(targetRole == null, CodeBindMessageEnums.PARAMS_ERROR, "目标角色不存在");
        ThrowUtils.throwIf(Objects.equals(loginUser.getUserRole(), targetRole.getCode()),
                CodeBindMessageEnums.PARAMS_ERROR, "当前用户已经是该角色了, 无需切换帐号");
        ThrowUtils.throwIf(!isAllowedRoleToggle(loginUser.getUserRole(), targetRole.getCode()),
                CodeBindMessageEnums.NO_AUTH_ERROR, "只允许教师帐号和专业负责人帐号互相切换");
        ThrowUtils.throwIf(StringUtils.isBlank(loginUser.getEmail()), CodeBindMessageEnums.USER_INIT_PASSWD,
                "本帐号必须先绑定邮箱");

        List<User> candidates = findCounterpart(loginUser, targetRole.getCode());
        ThrowUtils.throwIf(candidates.size() != 1, CodeBindMessageEnums.ILLEGAL_OPERATION_ERROR,
                "无法唯一确定要切换的帐号, 请联系管理员检查同名、同系、同邮箱的帐号数据");
        authSessionManager.logoutUser(loginUser.getId());
        User target = candidates.get(0);
        authSessionManager.login(target, device);
        return userService.getLoginUserVO(target);
    }

    @Override
    public RoleSwitchAvailabilityVO getRoleSwitchAvailability() {
        User loginUser = userService.userGetCurrentLoginUser();
        if (loginUser == null || StringUtils.isBlank(loginUser.getEmail())) {
            return new RoleSwitchAvailabilityVO(false, null);
        }
        for (UserRoleEnum candidate : new UserRoleEnum[]{UserRoleEnum.TEACHER, UserRoleEnum.DEPT}) {
            if (isAllowedRoleToggle(loginUser.getUserRole(), candidate.getCode())
                    && findCounterpart(loginUser, candidate.getCode()).size() == 1) {
                return new RoleSwitchAvailabilityVO(true, candidate.getDescription());
            }
        }
        return new RoleSwitchAvailabilityVO(false, null);
    }

    private List<User> findCounterpart(User loginUser, int targetRole) {
        return userService.list(new QueryWrapper<User>()
                .ne("id", loginUser.getId())
                .eq("status", "老用户")
                .eq("userName", loginUser.getUserName())
                .eq("dept", loginUser.getDept())
                .eq("email", loginUser.getEmail())
                .eq("userRole", targetRole));
    }

    private static boolean isAllowedRoleToggle(Integer currentRole, Integer targetRole) {
        return (Objects.equals(currentRole, UserRoleEnum.TEACHER.getCode())
                && Objects.equals(targetRole, UserRoleEnum.DEPT.getCode()))
                || (Objects.equals(currentRole, UserRoleEnum.DEPT.getCode())
                && Objects.equals(targetRole, UserRoleEnum.TEACHER.getCode()));
    }

    private static UserRoleEnum roleByDescription(String description) {
        for (UserRoleEnum role : UserRoleEnum.values()) {
            if (role.getDescription().equals(description)) {
                return role;
            }
        }
        return null;
    }
}
