package cn.com.edtechhub.worktopicselection.model.request.ai;

import lombok.Data;

import java.io.Serializable;

/**
 * AI 问答请求
 *
 * @author wobushi041
 */
@Data
public class AiSendRequest implements Serializable {

    /**
     * 消息内容
     */
    private String content;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
