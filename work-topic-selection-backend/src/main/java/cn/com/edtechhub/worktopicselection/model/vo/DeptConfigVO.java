package cn.com.edtechhub.worktopicselection.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 系部配置视图
 *
 * @author wobushi041
 */
@Data
public class DeptConfigVO implements Serializable {

    /**
     * 可选系部 ID 配置列表
     */
    private Map<String, List<String>> enableSelectDeptsList;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
