package cn.com.edtechhub.worktopicselection.model.dto.topic;

import cn.com.edtechhub.worktopicselection.model.dto.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 管理员查询题目请求
 *
 * @author wobushi041
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TopicQueryByAdminRequest extends PageRequest implements Serializable {

    /**
     * 题目
     */
    private String topic;

    /**
     * 题目类型
     */
    private String type;

    /**
     * 指导老师
     */
    private String teacherName;

    /**
     * 系部名
     */
    private String deptName;

    /**
     * 开启时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}