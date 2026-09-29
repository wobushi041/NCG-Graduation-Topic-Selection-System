package cn.edu.nfu.topicselection.model.request.topic;

import lombok.Data;

import java.io.Serializable;

/**
 * 获取题目审核等级请求
 *
 * @author wobushi041
 */
@Data
public class GetTopicReviewLevelRequest implements Serializable {

    /**
     * 题目要求
     */
    private String requirement;

    /**
     * 题目类型
     */
    private String type;

    /**
     * 题目标题
     */
    private String topic;

    /**
     * 题目描述
     */
    private String description;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
