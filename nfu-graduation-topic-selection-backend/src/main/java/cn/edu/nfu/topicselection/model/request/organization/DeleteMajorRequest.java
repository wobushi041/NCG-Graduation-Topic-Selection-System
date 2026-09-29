package cn.edu.nfu.topicselection.model.request.organization;

import lombok.Data;

import java.io.Serializable;

/**
 * 删除专业请求
 *
 * @author wobushi041
 */
@Data
public class DeleteMajorRequest implements Serializable {

    /**
     * 专业 id
     */
    private Long majorId;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
