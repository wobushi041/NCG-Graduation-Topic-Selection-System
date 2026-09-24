package cn.com.edtechhub.worktopicselection.manager.websocket;

import lombok.Getter;

/**
 * WebSocket 实时消息类型枚举
 *
 * @author wobushi041
 */
@Getter
public enum WebSocketMessageTypeEnum {

    /**
     * 错误消息
     */
    ERROR_MESSAGE(-1, "ERROR"),

    /**
     * 信息通知
     */
    INFO_MESSAGE(0, "INFO"),

    /**
     * 状态变更
     */
    CHANGE_MESSAGE(1, "STATUS_CHANGE")
    ;

    /**
     * 消息类型编码
     */
    private final int code;

    /**
     * 消息类型描述
     */
    private final String description;

    /**
     * 构造 WebSocket 消息类型枚举实例
     *
     * @param code        消息类型编码
     * @param description 消息类型描述
     */
    WebSocketMessageTypeEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据消息类型编码查找对应的枚举实例
     *
     * @param code 消息类型编码
     * @return 匹配的枚举实例，未匹配时返回 null
     */
    public static WebSocketMessageTypeEnum getEnumByCode(int code) {
        for (WebSocketMessageTypeEnum enumItem : WebSocketMessageTypeEnum.values()) {
            if (enumItem.getCode() == code) {
                return enumItem;
            }
        }
        return null;
    }

}
