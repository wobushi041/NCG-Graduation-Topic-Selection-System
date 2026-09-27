package cn.com.edtechhub.worktopicselection.model.request.organization;

import lombok.Data;

import java.io.Serializable;

/**
 * 专业创建请求
 *
 * @author wobushi041
 */
@Data
public class ProjectAddRequest implements Serializable {

    /**
     * 专业名称
     */
    private String projectName;

    /**
     * 系部名称
     */
    private String deptName;

    /**
     * 专业所属选题组（可在后台统一配置）
     */
    private String groupName;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
