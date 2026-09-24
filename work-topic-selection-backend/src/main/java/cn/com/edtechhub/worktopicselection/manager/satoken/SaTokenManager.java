package cn.com.edtechhub.worktopicselection.manager.satoken;

import cn.com.edtechhub.worktopicselection.constant.UserConstant;
import cn.com.edtechhub.worktopicselection.model.entity.User;
import cn.com.edtechhub.worktopicselection.model.enums.UserRoleEnum;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Sa-Token 权限认证与角色解析管理器
 *
 * @author wobushi041
 */
@Component
@Slf4j
public class SaTokenManager implements StpInterface {

    /**
     * 通过 Sa-Token 解析指定令牌并校验其绑定的登录用户 ID 是否匹配
     *
     * @param tokenValue 待校验的登录令牌字符串
     * @param loginId    期望匹配的登录用户 ID
     * @return 令牌有效且与指定用户 ID 匹配时返回 true，否则返回 false
     */
    public boolean isTokenValidForLoginId(String tokenValue, Long loginId) {
        if (tokenValue == null || loginId == null) {
            return false;
        }
        try {
            Object tokenLoginId = StpUtil.getLoginIdByToken(tokenValue);
            return tokenLoginId != null
                    && Objects.equals(String.valueOf(tokenLoginId), String.valueOf(loginId));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    /**
     * 从 Sa-Token 会话缓存中读取登录用户信息并解析其拥有的角色标识列表
     *
     * @param loginId   账号唯一登录标识
     * @param loginType 账号登录体系类型
     * @return 当前账号所拥有的角色标识字符串集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 制作空的角色标识集合
        List<String> list = new ArrayList<>();

        // 获取当前登录用户信息
        User user = (User) StpUtil.getSessionByLoginId(loginId).get(UserConstant.USER_LOGIN_STATE); // 直接从会话缓存中获取用户的所有信息
        UserRoleEnum userRole = user == null || user.getUserRole() == null
                ? null
                : UserRoleEnum.getEnums(user.getUserRole()); // 由于在本数据库中为了拓展性使用数字来标识身份，因此需要做一层转化

        // 返回角色标识集合
        if (userRole != null && userRole != UserRoleEnum.BAN_ROLE) {
            list.add(userRole.getDescription());
        }
        log.debug("本次调用用户携带的角色标识集合为: {}", list);
        return list;
    }

    /**
     * 获取指定登录账号所拥有的细粒度权限码列表
     *
     * @param loginId   账号唯一登录标识
     * @param loginType 账号登录体系类型
     * @return 当前账号所拥有的权限码字符串集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 制作空的权限码值集合
        List<String> list = new ArrayList<>();

        // 返回权限码值集合
        log.debug("本次调用用户携带的的权限码值集合为 {}", list);
        return list;
    }

}
