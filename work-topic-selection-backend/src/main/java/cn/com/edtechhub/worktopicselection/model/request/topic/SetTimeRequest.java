package cn.com.edtechhub.worktopicselection.model.request.topic;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 设置选题开放时间请求
 *
 * @author wobushi041
 */
@Data
public class SetTimeRequest implements Serializable {

    /**
     * 选题 id 列表
     */
    private List<Long> topicIds;

    /**
     * 开启时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
