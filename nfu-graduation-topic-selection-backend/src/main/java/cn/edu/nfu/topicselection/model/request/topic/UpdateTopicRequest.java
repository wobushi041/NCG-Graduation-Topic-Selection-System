package cn.edu.nfu.topicselection.model.request.topic;

import lombok.Data;

import java.io.Serializable;

/**
 * 修改题目请求
 *
 * @author wobushi041
 */
@Data
public class UpdateTopicRequest implements Serializable {

    /**
     * 需要修改的题目名字
     */
    private String topicName;

    /**
     * 题目类型
     */
    private String type;

    /**
     * 题目描述
     */
    private String description;

    /**
     * 对学生要求
     */
    private String requirement;

    /**
     * 所属选题组 id
     */
    private Long topicGroupId;

    /**
     * 可接收学生总容量
     */
    private Integer surplusQuantity;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
