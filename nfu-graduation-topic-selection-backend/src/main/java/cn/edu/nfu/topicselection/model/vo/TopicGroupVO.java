package cn.edu.nfu.topicselection.model.vo;

import lombok.Data;

/**
 * 选题组下拉选项
 *
 * @author wobushi041
 */
@Data
public class TopicGroupVO {

    /**
     * 选题组 id
     */
    private Long value;

    /**
     * 选题组名称
     */
    private String label;

    /**
     * 所属学院 id
     */
    private Long collegeId;

}
