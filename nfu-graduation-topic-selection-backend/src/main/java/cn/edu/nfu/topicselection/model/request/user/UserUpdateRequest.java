package cn.edu.nfu.topicselection.model.request.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户更新请求
 *
 * @author wobushi041
 */
@Data
public class UserUpdateRequest implements Serializable {

    /**
     * 用户 id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户角色
     */
    private Integer userRole;

    /**
     * 所属学院 id
     */
    private Long collegeId;

    /**
     * 所属专业 id
     */
    private Long majorId;

    /**
     * 负责的选题组 id
     */
    private Long topicGroupId;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
