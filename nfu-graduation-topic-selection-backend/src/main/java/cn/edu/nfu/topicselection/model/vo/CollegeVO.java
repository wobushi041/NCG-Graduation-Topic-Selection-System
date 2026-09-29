package cn.edu.nfu.topicselection.model.vo;

import lombok.Data;

/**
 * 学院下拉选项
 *
 * @author wobushi041
 */
@Data
public class CollegeVO {

    /**
     * 学院 id
     */
    private Long value;

    /**
     * 学院名称
     */
    private String label;

}
