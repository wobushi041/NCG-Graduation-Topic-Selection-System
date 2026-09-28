package cn.edu.nfu.topicselection.model.vo;

import lombok.Data;

/**
 * 系部教师脱敏类
 *
 * @author wobushi041
 */
@Data
public class DeptTeacherVO {

    /**
     * 教师名字
     */
    private String teacherName;

    /**
     * 系部名称
     */
    private String deptName;

    /**
     * 选题数量
     */
    private int topicAmount;

    /**
     * 预选人数
     */
    private int selectAmount;

    /**
     * 剩余数量
     */
    private int surplusQuantity;

}
