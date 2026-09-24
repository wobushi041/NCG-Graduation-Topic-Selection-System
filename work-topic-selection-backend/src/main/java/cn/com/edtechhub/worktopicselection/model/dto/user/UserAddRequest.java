package cn.com.edtechhub.worktopicselection.model.dto.user;

import com.baomidou.mybatisplus.annotation.TableField;
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
     * 系部
     */
    private String deptName;

    /**
     * 专业
     */
    private String project;

    /**
     * 角色
     */
    private Integer userRole;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}