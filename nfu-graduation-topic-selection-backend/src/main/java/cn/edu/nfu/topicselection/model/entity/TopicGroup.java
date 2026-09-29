package cn.edu.nfu.topicselection.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 选题组实体
 *
 * @author wobushi041
 */
@Data
@TableName("topic_group")
public class TopicGroup implements Serializable {

    /**
     * 选题组 id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属学院 id
     */
    private Long collegeId;

    /**
     * 选题组名称
     */
    private String groupName;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}
