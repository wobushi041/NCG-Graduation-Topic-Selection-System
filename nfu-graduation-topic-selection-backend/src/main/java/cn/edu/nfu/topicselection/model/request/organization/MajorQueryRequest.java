package cn.edu.nfu.topicselection.model.request.organization;

import cn.edu.nfu.topicselection.model.dto.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 专业查询请求
 *
 * @author wobushi041
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MajorQueryRequest extends PageRequest implements Serializable {

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
