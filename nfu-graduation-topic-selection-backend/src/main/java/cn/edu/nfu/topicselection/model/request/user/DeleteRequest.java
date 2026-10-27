package cn.edu.nfu.topicselection.model.request.user;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 用户删除请求
 *
 * @author wobushi041
 */
@Data
public class DeleteRequest implements Serializable {

    /**
     * 账号（因为用户账号本身就是唯一的，因此完全可以替代 id 值）
     */
    @NotBlank(message = "指定删除的用户账号不能为空")
    private String userAccount;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
