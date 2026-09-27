package cn.com.edtechhub.worktopicselection.model.request.selection;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 按题目 id 预选或确认选题请求
 *
 * @author wobushi041
 */
@Data
public class SelectTopicByIdRequest implements Serializable {

    /**
     * 题目 id
     */
    @NotNull(message = "题目 id 不能为空")
    private Long id;

    /**
     * 选题状态（0 - 预选，1 - 确认选题，2 - 取消预选）
     */
    @NotNull(message = "操作状态不能为空")
    private Integer status;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
