package cn.com.edtechhub.worktopicselection.manager.ai;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 接口事件流响应封装模型
 *
 * @author wobushi041
 */
@Data
public class AIResponse {

    /**
     * 第一个 reply 事件数据（用户提问回显）
     */
    private Map<String, Object> firstReply = new HashMap<>();

    /**
     * 第一个 token_stat 事件数据（模型开始处理状态）
     */
    private Map<String, Object> firstTokenStat = new HashMap<>();

    /**
     * 第二个 token_stat 事件数据（模型推理过程统计信息）
     */
    private Map<String, Object> secondTokenStat = new HashMap<>();

    /**
     * 第二个 reply 事件数据（模型最终回答内容）
     */
    private Map<String, Object> secondReply = new HashMap<>();

}
