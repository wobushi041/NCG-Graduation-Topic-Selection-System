package cn.com.edtechhub.worktopicselection.model.dto.user;

import cn.com.edtechhub.worktopicselection.model.dto.PageRequest;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

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
    private Long topicId;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}
