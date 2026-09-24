package cn.com.edtechhub.worktopicselection.constant;

/**
 * 用户模块常量接口
 *
 * @author wobushi041
 */
public interface UserConstant {

    /**
     * 用户账号最大长度
     */
    int MAX_USER_ACCOUNT_LENGTH = 128;

    /**
     * 旧版 MD5 密码使用的盐值（仅用于兼容登录并迁移到 BCrypt）
     */
    String LEGACY_PASSWORD_SALT = "edtechhub";

    /**
     * 用户登录态会话键名
     */
    String USER_LOGIN_STATE = "user_login";

    /**
     * 已验证邮箱会话键名
     */
    String VERIFIED_EMAIL_SESSION_KEY = "verified_email";

    /**
     * 已验证邮箱过期时间会话键名
     */
    String VERIFIED_EMAIL_EXPIRES_AT_SESSION_KEY = "verified_email_expires_at";

}
