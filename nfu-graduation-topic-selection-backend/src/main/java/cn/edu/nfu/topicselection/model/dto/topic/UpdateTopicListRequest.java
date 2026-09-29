package cn.edu.nfu.topicselection.model.dto.topic;

import lombok.Data;

import java.io.Serializable;

/**
 * 修改题目列表请求
 *
 * @author wobushi041
 */
@Data
public class UpdateTopicListRequest implements Serializable {

    /**
     * 题目 id
     */
    private Long id;

    /**
     * 题目
     */
    private String topic;

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
     * 选题组 id
     */
    private Long topicGroupId;

    /**
     * 指导老师
     */
    private String teacherName;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
