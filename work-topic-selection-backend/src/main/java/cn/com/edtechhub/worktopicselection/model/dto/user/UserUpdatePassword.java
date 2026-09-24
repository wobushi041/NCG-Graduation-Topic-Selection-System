package cn.com.edtechhub.worktopicselection.model.dto.user;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户修改密码请求
 *
 * @author wobushi041
 */
@Data
public class UserUpdatePassword implements Serializable {

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 旧的密码
     */
    private String userPassword;

    /**
     * 新的密码
     */
    private String updatePassword;

    /**
     * 临时密码
     */
    private String code;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}
