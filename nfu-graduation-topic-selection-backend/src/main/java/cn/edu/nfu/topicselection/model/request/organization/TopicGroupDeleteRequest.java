package cn.edu.nfu.topicselection.model.request.organization;

import lombok.Data;

import java.io.Serializable;

/**
 * 选题组删除请求
 *
 * @author wobushi041
 */
@Data
public class TopicGroupDeleteRequest implements Serializable {

    /**
     * 选题组 id
     */
    private Long id;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
