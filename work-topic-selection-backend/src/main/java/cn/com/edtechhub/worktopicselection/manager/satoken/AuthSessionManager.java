package cn.com.edtechhub.worktopicselection.manager.satoken;

import cn.com.edtechhub.worktopicselection.constant.UserConstant;
import cn.com.edtechhub.worktopicselection.model.entity.User;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

/**
 * 集中封装 Sa-Token 会话的主动操作
 *
 * @author wobushi041
 */
@Component
public class AuthSessionManager {

    /**
     * 使用 Sa-Token 创建用户会话并保存登录状态
     *
     * @param user   登录用户
     * @param device 登录设备类型
     */
    public void login(User user, String device) {
        StpUtil.login(user.getId(), device);
        StpUtil.getSession().set(UserConstant.USER_LOGIN_STATE, user);
    }

    /**
     * 注销当前用户的 Sa-Token 会话
     */
    public void logoutCurrent() {
        StpUtil.logout();
    }

    /**
     * 注销指定用户的全部 Sa-Token 会话
     *
     * @param userId 用户 id
     */
    public void logoutUser(Long userId) {
        StpUtil.logout(userId);
    }

}
