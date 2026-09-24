package cn.com.edtechhub.worktopicselection.exception;

import lombok.Getter;

/**
 * 业务异常类
 *
 * @author wobushi041
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码与消息绑定枚举
     */
    CodeBindMessageEnums codeBindMessageEnums;

    /**
     * 异常详细消息
     */
    String exceptionMessage;

    /**
     * 构造业务异常实例
     *
     * @param codeBindMessageEnums 错误码与消息绑定枚举
     * @param exceptionMessage     异常详细消息
     */
    public BusinessException(CodeBindMessageEnums codeBindMessageEnums, String exceptionMessage) {
        super(exceptionMessage);
        this.codeBindMessageEnums = codeBindMessageEnums;
        this.exceptionMessage = exceptionMessage;
    }

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}
