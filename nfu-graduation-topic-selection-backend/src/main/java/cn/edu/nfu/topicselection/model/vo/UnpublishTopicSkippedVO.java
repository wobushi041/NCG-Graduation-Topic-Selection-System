package cn.edu.nfu.topicselection.model.vo;

import lombok.Data;

/**
 * 取消发布时被跳过的课题及原因视图
 *
 * @author wobushi041
 */
@Data
public class UnpublishTopicSkippedVO {

    /**
     * 课题 id
     */
    private Long topicId;

    /**
     * 课题名称
     */
    private String topicName;

    /**
     * 指导教师姓名
     */
    private String teacherName;

    /**
     * 有效学生选题数量
     */
    private Long activeSelectionCount;

    /**
     * 跳过取消发布的原因
     */
    private String reason;

}
