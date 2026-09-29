package cn.edu.nfu.topicselection.model.request.user;

import cn.edu.nfu.topicselection.model.dto.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 教师查询请求
 *
 * @author wobushi041
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TopicLeaderQueryRequest extends PageRequest implements Serializable {

    /**
     * 教师姓名
     */
    private String teacherName;

    /**
     * 所属学院 id
     */
    private Long collegeId;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
