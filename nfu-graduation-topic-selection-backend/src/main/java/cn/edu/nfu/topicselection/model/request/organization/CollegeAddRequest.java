package cn.edu.nfu.topicselection.model.request.organization;

import lombok.Data;

import java.io.Serializable;

/**
 * 学院创建请求
 *
 * @author wobushi041
 */
@Data
public class CollegeAddRequest implements Serializable {

    /**
     * 学院名称
     */
    private String collegeName;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
