package cn.com.edtechhub.worktopicselection.model.request.user;

import lombok.Data;

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
    private String userAccount;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
