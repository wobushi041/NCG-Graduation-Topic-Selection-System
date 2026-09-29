package cn.edu.nfu.topicselection.model.request.organization;

import lombok.Data;

import java.io.Serializable;

/**
 * 选题组更新请求
 *
 * @author wobushi041
 */
@Data
public class TopicGroupUpdateRequest implements Serializable {

    /**
     * 选题组 id
     */
    private Long id;

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
