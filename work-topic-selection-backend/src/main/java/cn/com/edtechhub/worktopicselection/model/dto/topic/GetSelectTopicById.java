package cn.com.edtechhub.worktopicselection.model.dto.topic;

import lombok.Data;

import java.io.Serializable;

/**
 * 按题目 id 获取已选题目请求
 *
 * @author wobushi041
 */
@Data
public class GetSelectTopicById implements Serializable {

    /**
     * 题目 id
     */
    private long id;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}