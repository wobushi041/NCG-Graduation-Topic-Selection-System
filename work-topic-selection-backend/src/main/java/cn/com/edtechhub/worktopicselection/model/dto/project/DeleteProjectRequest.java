package cn.com.edtechhub.worktopicselection.model.dto.project;

import lombok.Data;

import java.io.Serializable;

/**
 * 删除专业请求
 *
 * @author wobushi041
 */
@Data
public class DeleteProjectRequest implements Serializable {

    /**
     * 专业名称
     */
    private String projectName;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}