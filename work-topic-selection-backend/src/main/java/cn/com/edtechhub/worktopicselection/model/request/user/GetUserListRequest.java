package cn.com.edtechhub.worktopicselection.model.request.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理员请求用户数据
 *
 * @author wobushi041
 */
@Data
public class GetUserListRequest implements Serializable {

    /**
     * 用户角色 0 - 普通用户 1 - 教师 2 - 系部 3 - 管理员
     */
    private Integer userRole;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
