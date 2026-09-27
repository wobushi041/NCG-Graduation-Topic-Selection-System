package cn.com.edtechhub.worktopicselection.model.request.organization;

import cn.com.edtechhub.worktopicselection.model.dto.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 专业查询请求
 *
 * @author wobushi041
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ProjectQueryRequest extends PageRequest implements Serializable {

    /**
     * 专业名称
     */
    private String projectName;

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
