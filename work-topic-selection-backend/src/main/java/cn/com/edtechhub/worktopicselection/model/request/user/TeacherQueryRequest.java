package cn.com.edtechhub.worktopicselection.model.request.user;

import lombok.Data;

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
    private int userRole;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
