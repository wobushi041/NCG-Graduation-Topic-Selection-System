package cn.edu.nfu.topicselection.model.request.policy;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 设置学院跨选配置请求
 *
 * @author wobushi041
 */
@Data
public class SetCollegeConfigRequest implements Serializable {

    /**
     * 可选学院 ID 配置列表，键为源学院 id，值为允许选择的目标学院 id
     */
    private Map<String, List<Long>> enableSelectCollegesList;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
