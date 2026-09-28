package cn.edu.nfu.topicselection.model.request.organization;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新专业选题组请求
 *
 * @author wobushi041
 */
@Data
public class ProjectGroupUpdateRequest implements Serializable {

    /**
     * 专业名称
     */
    private String projectName;

    /**
     * 选题组名称（为空表示取消该专业的分组配置）
     */
    private String groupName;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
