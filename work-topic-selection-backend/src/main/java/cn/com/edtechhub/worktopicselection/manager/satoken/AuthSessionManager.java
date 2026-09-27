package cn.com.edtechhub.worktopicselection.manager.satoken;

import cn.com.edtechhub.worktopicselection.constant.UserConstant;
import cn.com.edtechhub.worktopicselection.model.entity.User;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

/**
 * 集中封装 Sa-Token 会话的主动操作。
 */
@Component
public class AuthSessionManager {

    public void login(User user, String device) {
        StpUtil.login(user.getId(), device);
        StpUtil.getSession().set(UserConstant.USER_LOGIN_STATE, user);
    }

    public void logoutCurrent() {
        StpUtil.logout();
    }

    public void logoutUser(Long userId) {
        StpUtil.logout(userId);
    }
}
