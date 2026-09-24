package cn.com.edtechhub.worktopicselection.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户名字脱敏类
 *
 * @author wobushi041
 */
@Data
public class UserNameVO implements Serializable {

    /**
     * 用户名
     */
    private String userName;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}