package cn.edu.nfu.topicselection.model.dto.user;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户切换角色请求
 *
 * @author wobushi041
 */
@Data
public class UserToggleRequest implements Serializable {

    /**
     * 想要切换的权限
     */
    private Integer userRole;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}
