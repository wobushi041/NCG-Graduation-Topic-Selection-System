package cn.com.edtechhub.worktopicselection.model.request.topic;

import lombok.Data;

import java.io.Serializable;

/**
 * 设置教师题目上限请求
 *
 * @author wobushi041
 */
@Data
public class SetTeacherTopicAmountRequest implements Serializable {

    /**
     * 教师 ID
     */
    private Long teacherId;

    /**
     * 题目上限数量
     */
    private Integer topicAmount;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
