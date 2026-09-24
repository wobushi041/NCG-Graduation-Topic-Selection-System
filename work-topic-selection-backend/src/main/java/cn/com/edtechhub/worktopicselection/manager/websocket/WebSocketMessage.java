package cn.com.edtechhub.worktopicselection.manager.websocket;

import cn.hutool.json.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 实时通信消息传输载荷模型
 *
 * @author wobushi041
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {

    /**
     * 消息类型编码（-1 - 错误消息，0 - 信息通知，1 - 状态变更）
     */
    private Integer typeCode;

    /**
     * 消息文本内容
     */
    private String message;

    /**
     * 消息扩展 JSON 数据载荷
     */
    private JSONObject extend;

}
