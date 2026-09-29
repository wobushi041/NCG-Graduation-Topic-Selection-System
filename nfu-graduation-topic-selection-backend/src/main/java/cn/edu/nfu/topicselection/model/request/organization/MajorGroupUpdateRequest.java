package cn.edu.nfu.topicselection.model.request.organization;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新专业选题组请求
 *
 * @author wobushi041
 */
@Data
public class MajorGroupUpdateRequest implements Serializable {

    /**
     * 专业 id
     */
    private Long majorId;

    /**
     * 选题组 id
     */
    private Long topicGroupId;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
