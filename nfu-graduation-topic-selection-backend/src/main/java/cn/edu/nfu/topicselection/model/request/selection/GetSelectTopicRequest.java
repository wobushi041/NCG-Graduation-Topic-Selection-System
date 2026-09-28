package cn.edu.nfu.topicselection.model.request.selection;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.io.Serializable;

/**
 * 查询最终题目选中时间记录请求
 *
 * @author wobushi041
 */
@Data
public class GetSelectTopicRequest implements Serializable {

    /**
     * 题目 id
     */
    @NotNull(message = "id 不能为空")
    @Positive(message = "id 必须是正整数")
    private Long topicId;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
