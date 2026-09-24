package cn.com.edtechhub.worktopicselection.model.dto.dept;

import lombok.Data;

import java.io.Serializable;

/**
 * 删除系部请求
 *
 * @author wobushi041
 */
@Data
public class DeleteDeptRequest implements Serializable {

    /**
     * 系部名称
     */
    private String deptName;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}