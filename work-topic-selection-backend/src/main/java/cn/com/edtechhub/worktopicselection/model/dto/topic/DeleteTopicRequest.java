package cn.com.edtechhub.worktopicselection.model.dto.topic;

import lombok.Data;

import java.io.Serializable;

/**
 * 删除题目请求
 *
 * @author wobushi041
 */
@Data
public class DeleteTopicRequest implements Serializable {

    /**
     * 题目 id
     */
    private Long id;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}