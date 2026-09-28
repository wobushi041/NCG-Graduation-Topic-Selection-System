package cn.edu.nfu.topicselection.model.request.topic;

import lombok.Data;

import java.io.Serializable;

/**
 * 添加题目请求
 *
 * @author wobushi041
 */
@Data
public class AddTopicRequest implements Serializable {

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
     * 系部名
     */
    private String deptName;

    /**
     * 系部主任
     */
    private String deptTeacher;

    /**
     * 题目适用的选题组（为空时兼容历史题目）
     */
    private String topicGroup;

    /**
     * 指导老师
     */
    private String teacherName;

    /**
     * 总数
     */
    private Integer amount;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
