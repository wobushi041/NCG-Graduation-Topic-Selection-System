package cn.com.edtechhub.worktopicselection.model.dto.studentTopicSelection;

import lombok.Data;

import java.io.Serializable;

/**
 * 选择学生请求
 *
 * @author wobushi041
 */
@Data
public class SelectStudentRequest implements Serializable {

    /**
     * 学生账号
     */
    private String userAccount;

    /**
     * 题目
     */
    private String topic;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}