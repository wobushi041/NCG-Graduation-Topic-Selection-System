package cn.edu.nfu.topicselection.model.request.topic;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 取消设置选题开放时间请求
 *
 * @author wobushi041
 */
@Data
public class UnSetTimeRequest implements Serializable {

    /**
     * 选题 id 列表
     */
    private List<Long> topicIds;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
