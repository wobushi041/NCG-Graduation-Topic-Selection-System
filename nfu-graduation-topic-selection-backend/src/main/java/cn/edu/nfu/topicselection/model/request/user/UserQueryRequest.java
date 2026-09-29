package cn.edu.nfu.topicselection.model.request.user;

import cn.edu.nfu.topicselection.model.dto.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 用户查询请求
 *
 * @author wobushi041
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserQueryRequest extends PageRequest implements Serializable {

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

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
     * 用户角色（0 - 学生，1 - 教师，2 - 选题负责人，3 - 管理员）
     */
    private Integer userRole;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
