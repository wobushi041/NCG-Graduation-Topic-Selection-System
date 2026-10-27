package cn.edu.nfu.topicselection.model.request.user;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
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
    @NotBlank(message = "缺少用户名字")
    private String userName;

    /**
     * 账号
     */
    @NotBlank(message = "缺少用户的学号/工号")
    @Size(max = 128, message = "用户账号不能超过 128 个字符")
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
    @NotNull(message = "缺少用户角色")
    private Integer userRole;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
