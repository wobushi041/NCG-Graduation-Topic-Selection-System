package cn.edu.nfu.topicselection.model.request.organization;

import cn.edu.nfu.topicselection.model.dto.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 学院查询请求
 *
 * @author wobushi041
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CollegeQueryRequest extends PageRequest implements Serializable {

    /**
     * 学院名称
     */
    private String collegeName;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
