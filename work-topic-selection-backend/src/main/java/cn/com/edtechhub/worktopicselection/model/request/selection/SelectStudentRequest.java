package cn.com.edtechhub.worktopicselection.model.request.selection;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 教师选择学生确认课题请求
 *
 * @author wobushi041
 */
@Data
public class SelectStudentRequest implements Serializable {

    /**
     * 学生账号
     */
    @NotBlank(message = "用户账号不能为空")
    private String userAccount;

    /**
     * 课题名称
     */
    @NotBlank(message = "课题名称不能为空")
    private String topic;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
