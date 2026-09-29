package cn.edu.nfu.topicselection.model.request.organization;

import lombok.Data;

import java.io.Serializable;

/**
 * 教师选题组额度更新请求
 *
 * @author wobushi041
 */
@Data
public class TeacherGroupQuotaUpdateRequest implements Serializable {

    /**
     * 教师账号
     */
    private String teacherAccount;

    /**
     * 选题组 id
     */
    private Long topicGroupId;

    /**
     * 最大出题数量
     */
    private Integer maxTopics;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
