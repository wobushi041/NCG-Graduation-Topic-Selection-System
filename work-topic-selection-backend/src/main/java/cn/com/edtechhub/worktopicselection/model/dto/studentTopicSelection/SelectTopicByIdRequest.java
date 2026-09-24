package cn.com.edtechhub.worktopicselection.model.dto.studentTopicSelection;

import lombok.Data;

import java.io.Serializable;

/**
 * 按题目 id 选题请求
 *
 * @author wobushi041
 */
@Data
public class SelectTopicByIdRequest implements Serializable {

    /**
     * 题目 id
     */
    private Long id;

    /**
     * 选题状态（1 - 表示选题，0 - 表示退选）
     */
    private int status;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}