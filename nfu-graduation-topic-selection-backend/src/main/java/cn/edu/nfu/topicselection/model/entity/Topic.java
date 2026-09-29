package cn.edu.nfu.topicselection.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 选题实体
 *
 * @author wobushi041
 */
@Data
@TableName("topic")
public class Topic implements Serializable {

    /**
     * 题目 id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 题目名称
     */
    private String topic;

    /**
     * 题目类型
     */
    private String type;

    /**
     * 题目描述
     */
    private String description;

    /**
     * 对学生要求
     */
    private String requirement;

    /**
     * 指导教师姓名
     */
    private String teacherName;

    /**
     * 指导教师账号
     */
    @JsonIgnore
    private String teacherAccount;

    /**
     * 所属选题组 id
     */
    private Long topicGroupId;

    /**
     * 所属选题组名称
     */
    @TableField(exist = false)
    private String topicGroupName;

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

    /**
     * 剩余可选余量
     */
    private Integer surplusQuantity;

    /**
     * 可接收学生总容量
     */
    private Integer capacity;

    /**
     * 开启时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 发布状态（-2 - 被打回，-1 - 待审核，0 - 未发布，1 - 已发布）
     */
    private Integer status;

    /**
     * 预选人数
     */
    private Integer selectAmount;

    /**
     * 打回理由
     */
    private String reason;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}
