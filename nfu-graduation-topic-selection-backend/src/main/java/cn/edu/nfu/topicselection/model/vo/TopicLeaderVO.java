package cn.edu.nfu.topicselection.model.vo;

import lombok.Data;

/**
 * 教师选题统计视图
 *
 * @author wobushi041
 */
@Data
public class TopicLeaderVO {

    /**
     * 教师名字
     */
    private String teacherName;

    /**
     * 所属学院 id
     */
    private Long collegeId;

    /**
     * 所属学院名称
     */
    private String collegeName;

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
