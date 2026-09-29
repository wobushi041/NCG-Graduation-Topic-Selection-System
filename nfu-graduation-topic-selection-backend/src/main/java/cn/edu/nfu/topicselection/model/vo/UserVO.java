package cn.edu.nfu.topicselection.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户脱敏类
 *
 * @author wobushi041
 */
@Data
public class UserVO implements Serializable {

    /**
     * 用户 id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar = "/logo_128.png";

    /**
     * 用户角色（user / admin / ban）
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

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 状态
     */
    private String status;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
