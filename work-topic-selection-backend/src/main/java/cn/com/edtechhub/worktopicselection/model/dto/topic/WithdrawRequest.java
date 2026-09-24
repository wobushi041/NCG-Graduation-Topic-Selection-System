package cn.com.edtechhub.worktopicselection.model.dto.topic;

import lombok.Data;

import java.io.Serializable;

/**
 * 退选题目请求
 *
 * @author wobushi041
 */
@Data
public class WithdrawRequest implements Serializable {

    /**
     * 题目 id
     */
    private Long id;

    /**
     * 学生账号（教师操作时必填，学生操作时由服务端使用当前登录账号）
     */
    private String userAccount;

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
