package cn.com.edtechhub.worktopicselection.model.dto.topic;

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
     * 题目适用的选题组（为空时兼容历史题目）
     */
    private String topicGroup;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
