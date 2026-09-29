package cn.edu.nfu.topicselection.model.request.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户添加请求
 *
 * @author wobushi041
 */
@Data
public class UserAddRequest implements Serializable {

    /**
     * 姓名
     */
    private String userName;

    /**
     * 账号
     */
    private String userAccount;

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

    /**
     * 角色
     */
    private Integer userRole;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
