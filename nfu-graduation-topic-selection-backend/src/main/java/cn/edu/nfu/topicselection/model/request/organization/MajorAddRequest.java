package cn.edu.nfu.topicselection.model.request.organization;

import lombok.Data;

import java.io.Serializable;

/**
 * 专业创建请求
 *
 * @author wobushi041
 */
@Data
public class MajorAddRequest implements Serializable {

    /**
     * 专业名称
     */
    private String majorName;

    /**
     * 所属学院 id
     */
    private Long collegeId;

    /**
     * 所属选题组 id
     */
    private Long topicGroupId;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
