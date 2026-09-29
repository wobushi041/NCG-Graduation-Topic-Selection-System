package cn.edu.nfu.topicselection.model.request.organization;

import cn.edu.nfu.topicselection.model.dto.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 选题组查询请求
 *
 * @author wobushi041
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TopicGroupQueryRequest extends PageRequest implements Serializable {

    /**
     * 所属学院 id
     */
    private Long collegeId;

    /**
     * 选题组名称
     */
    private String groupName;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
