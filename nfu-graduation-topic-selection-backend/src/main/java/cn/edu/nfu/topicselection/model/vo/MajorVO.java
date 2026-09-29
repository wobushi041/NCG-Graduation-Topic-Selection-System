package cn.edu.nfu.topicselection.model.vo;

import lombok.Data;

/**
 * 专业下拉选项
 *
 * @author wobushi041
 */
@Data
public class MajorVO {

    /**
     * 专业 id
     */
    private Long value;

    /**
     * 专业标签
     */
    private String label;

}
