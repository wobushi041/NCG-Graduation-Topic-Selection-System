package cn.edu.nfu.topicselection.exception;

import lombok.Getter;

/**
 * 错误码与提示消息绑定枚举
 *
 * @author wobushi041
 */
@Getter
public enum CodeBindMessageEnums {

    /**
     * 请求处理成功
     */
    SUCCESS(0, "成功"),

    /**
     * 请求参数错误
     */
    PARAMS_ERROR(40000, "参数错误"),

    /**
     * 用户首次登录需修改初始密码
     */
    USER_INIT_PASSWD(40001, "用户首次登陆"),

    /**
     * 未登录或登录认证失败
     */
    NO_LOGIN_ERROR(40100, "登录认证错误"),

    /**
     * 角色权限不匹配
     */
    NO_ROLE_ERROR(40101, "角色认证错误"),

    /**
     * 操作权限不足
     */
    NO_AUTH_ERROR(40102, "权限认证错误"),

    /**
     * 账号已被封禁
     */
    USER_DISABLE_ERROR(40103, "账号封禁错误"),

    /**
     * 禁止访问目标资源
     */
    FORBIDDEN_ERROR(40300, "禁止访问的资源"),

    /**
     * 非法操作请求
     */
    ILLEGAL_OPERATION_ERROR(40301, "您做了非法操作"),

    /**
     * 请求的资源不存在
     */
    NOT_FOUND_ERROR(40400, "请求不存在资源"),

    /**
     * 请求处理超时
     */
    TIMEOUT_ERROR(40800, "请求超时"),

    /**
     * 系统内部异常
     */
    SYSTEM_ERROR(50000, "系统内部错误"),

    /**
     * 业务操作执行失败
     */
    OPERATION_ERROR(50001, "操作失败"),

    /**
     * 触发 Sentinel 流量控制规则
     */
    FLOW_RULES(50002, "触发流量控制规则"),

    /**
     * 触发 Sentinel 熔断降级规则
     */
    DEGRADE_RULES(50003, "触发熔断降级规则"),

    /**
     * 触发 Sentinel 热点参数规则
     */
    PARAM_RULES(50004, "触发热点参数规则"),

    /**
     * 触发 Sentinel 系统保护规则
     */
    SYSTEM_RULES(50005, "触发系统保护规则"),
    ;

    /**
     * 状态码
     */
    private final int code;

    /**
     * 提示消息
     */
    private final String message;

    /**
     * 构造错误码与提示消息枚举项
     *
     * @param code    状态码
     * @param message 提示消息
     */
    CodeBindMessageEnums(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
