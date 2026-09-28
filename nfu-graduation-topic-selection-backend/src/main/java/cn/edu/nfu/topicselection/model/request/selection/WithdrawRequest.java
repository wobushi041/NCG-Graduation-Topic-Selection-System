package cn.edu.nfu.topicselection.model.request.selection;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
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
    @NotNull(message = "id 不能为空")
    @Positive(message = "id 必须是正整数")
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
