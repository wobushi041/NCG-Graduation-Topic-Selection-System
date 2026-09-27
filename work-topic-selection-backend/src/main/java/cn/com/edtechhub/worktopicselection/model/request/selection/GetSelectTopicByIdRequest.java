package cn.com.edtechhub.worktopicselection.model.request.selection;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.io.Serializable;

/**
 * 按题目 id 查询已选学生列表请求
 *
 * @author wobushi041
 */
@Data
public class GetSelectTopicByIdRequest implements Serializable {

    /**
     * 题目 id
     */
    @NotNull(message = "id 不能为空")
    @Positive(message = "id 必须是正整数")
    private Long id;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
