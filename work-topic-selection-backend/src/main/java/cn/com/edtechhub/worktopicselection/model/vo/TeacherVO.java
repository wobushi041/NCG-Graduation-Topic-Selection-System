package cn.com.edtechhub.worktopicselection.model.vo;

import lombok.Data;

/**
 * 教师脱敏类
 *
 * @author wobushi041
 */
@Data
public class TeacherVO {

    /**
     * 教师值
     */
    private String value;

    /**
     * 教师标签
     */
    private String label;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
