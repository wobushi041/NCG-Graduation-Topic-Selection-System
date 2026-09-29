package cn.edu.nfu.topicselection.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户实体
 *
 * @author wobushi041
 */
@Data
@TableName("user")
public class User implements Serializable {

    /**
     * 用户 id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户姓名
     */
    private String userName;

    /**
     * 密码摘要
     */
    @JsonIgnore
    @ToString.Exclude
    private String userPassword;

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
     * 用户角色（0 - 学生，1 - 教师，2 - 选题负责人，3 - 系统）
     */
    private Integer userRole;

    /**
     * 所属学院 id
     */
    private Long collegeId;

    /**
     * 所属专业 id
     */
    private Long majorId;

    /**
     * 负责的选题组 id
     */
    private Long topicGroupId;

    /**
     * 账号状态
     */
    private String status;

    /**
     * 已选课题数量或最大出题数量
     */
    private Integer topicAmount;

    /**
     * 验证码发送邮箱
     */
    @JsonIgnore
    @ToString.Exclude
    private String email;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}
