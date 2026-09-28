package cn.edu.nfu.topicselection.service.impl;

import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import cn.edu.nfu.topicselection.manager.satoken.AuthSessionManager;
import cn.edu.nfu.topicselection.manager.security.SecurityRateLimitManager;
import cn.edu.nfu.topicselection.model.entity.User;
import cn.edu.nfu.topicselection.model.enums.UserRoleEnum;
import cn.edu.nfu.topicselection.model.request.auth.LoginRequest;
import cn.edu.nfu.topicselection.model.request.auth.RoleSwitchRequest;
import cn.edu.nfu.topicselection.model.vo.LoginUserVO;
import cn.edu.nfu.topicselection.model.vo.RoleSwitchAvailabilityVO;
import cn.edu.nfu.topicselection.service.AuthenticationService;
import cn.edu.nfu.topicselection.service.PasswordService;
import cn.edu.nfu.topicselection.service.UserService;
import cn.edu.nfu.topicselection.utils.ThrowUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 基于数据库账号、BCrypt 密码和 Sa-Token 会话实现认证业务
 *
 * @author wobushi041
 */
@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    /**
     * 未知账号密码校验使用的伪 BCrypt 摘要
     */
    private static final String DUMMY_PASSWORD_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    /**
     * 注入用户服务依赖
     */
    private final UserService userService;

    /**
     * 注入密码服务依赖
     */
    private final PasswordService passwordService;

    /**
     * 注入安全业务限频管理器依赖
     */
    private final SecurityRateLimitManager rateLimitManager;

    /**
     * 注入认证会话管理器依赖
     */
    private final AuthSessionManager authSessionManager;

    /**
     * 初始化认证服务实现
     *
     * @param userService        用户服务
     * @param passwordService    密码服务
     * @param rateLimitManager   安全业务限频管理器
     * @param authSessionManager 认证会话管理器
     */
    public AuthenticationServiceImpl(UserService userService, PasswordService passwordService,
                                     SecurityRateLimitManager rateLimitManager,
                                     AuthSessionManager authSessionManager) {
        this.userService = userService;
        this.passwordService = passwordService;
        this.rateLimitManager = rateLimitManager;
        this.authSessionManager = authSessionManager;
    }

    /// 登录会话 ///

    /**
     * 通过数据库账号和 BCrypt 兼容校验建立 Sa-Token 会话
     *
     * @param request  登录请求
     * @param clientIp 客户端 IP
     * @param device   登录设备类型
     * @return 登录用户信息
     */
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

    /**
     * 通过 Sa-Token 注销当前登录会话
     *
     * @return 是否注销成功
     */
    @Override
    public boolean logout() {
        authSessionManager.logoutCurrent();
        return true;
    }

    /// 角色切换 ///

    /**
     * 根据同名、同系和同邮箱账号匹配结果切换 Sa-Token 会话
     *
     * @param request 角色切换请求
     * @param device  登录设备类型
     * @return 切换后的登录用户信息
     */
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

    /**
     * 查询数据库中的配对账号并计算角色切换可用性
     *
     * @return 角色切换可用性
     */
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

    /**
     * 按身份配对规则查询目标角色账号
     *
     * @param loginUser  当前登录用户
     * @param targetRole 目标角色编码
     * @return 符合配对条件的用户列表
     */
    private List<User> findCounterpart(User loginUser, int targetRole) {
        return userService.list(new QueryWrapper<User>()
                .ne("id", loginUser.getId())
                .eq("status", "老用户")
                .eq("userName", loginUser.getUserName())
                .eq("dept", loginUser.getDept())
                .eq("email", loginUser.getEmail())
                .eq("userRole", targetRole));
    }

    /**
     * 判断当前角色和目标角色是否构成允许的切换组合
     *
     * @param currentRole 当前角色编码
     * @param targetRole  目标角色编码
     * @return 是否允许切换
     */
    private static boolean isAllowedRoleToggle(Integer currentRole, Integer targetRole) {
        return (Objects.equals(currentRole, UserRoleEnum.TEACHER.getCode())
                && Objects.equals(targetRole, UserRoleEnum.DEPT.getCode()))
                || (Objects.equals(currentRole, UserRoleEnum.DEPT.getCode())
                && Objects.equals(targetRole, UserRoleEnum.TEACHER.getCode()));
    }

    /**
     * 根据角色描述查找角色枚举
     *
     * @param description 角色描述
     * @return 匹配的角色枚举
     */
    private static UserRoleEnum roleByDescription(String description) {
        for (UserRoleEnum role : UserRoleEnum.values()) {
            if (role.getDescription().equals(description)) {
                return role;
            }
        }
        return null;
    }

}
