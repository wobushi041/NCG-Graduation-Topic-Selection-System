package cn.edu.nfu.topicselection.model.request.user;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 教师查询请求
 *
 * @author wobushi041
 */
@Data
public class TeacherQueryRequest implements Serializable {

    /**
     * 用户角色
     */
    @NotNull(message = "用户角色不能为空")
    private Integer userRole;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
