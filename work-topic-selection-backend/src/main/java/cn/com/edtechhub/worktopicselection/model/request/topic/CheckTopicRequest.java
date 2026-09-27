package cn.com.edtechhub.worktopicselection.model.request.topic;

import lombok.Data;

import java.io.Serializable;

/**
 * 题目审核请求
 *
 * @author wobushi041
 */
@Data
public class CheckTopicRequest implements Serializable {

    /**
     * 题目 id
     */
    private Long id;

    /**
     * 题目状态
     */
    private Integer status;

    /**
     * 审核理由
     */
    private String reason;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
