package cn.edu.nfu.topicselection.model.request.user;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 管理员请求用户数据
 *
 * @author wobushi041
 */
@Data
public class GetUserListRequest implements Serializable {

    /**
     * 用户角色（0 - 学生，1 - 教师，2 - 选题负责人，3 - 管理员）
     */
    @NotNull(message = "用户角色不存在")
    private Integer userRole;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
