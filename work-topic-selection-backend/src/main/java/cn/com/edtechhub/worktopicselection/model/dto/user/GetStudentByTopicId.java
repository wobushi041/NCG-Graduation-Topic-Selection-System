package cn.com.edtechhub.worktopicselection.model.dto.user;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;

/**
 * 获取学生列表请求
 *
 * @author wobushi041
 */
@Data
public class GetStudentByTopicId implements Serializable {

    /**
     * 题目 id
     */
    private long id;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}