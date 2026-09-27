package cn.com.edtechhub.worktopicselection.model.request.topic;

import lombok.Data;

import java.io.Serializable;

/**
 * 获取教师题目上限请求
 *
 * @author wobushi041
 */
@Data
public class GetTeacherTopicAmountRequest implements Serializable {

    /**
     * 教师 ID
     */
    private Long teacherId;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
