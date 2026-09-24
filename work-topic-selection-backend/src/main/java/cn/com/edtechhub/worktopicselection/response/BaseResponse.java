package cn.com.edtechhub.worktopicselection.response;

import cn.com.edtechhub.worktopicselection.exception.CodeBindMessageEnums;
import lombok.Data;

import java.io.Serializable;

/**
 * 通用接口响应体封装类
 *
 * @author wobushi041
 */
@Data
public class BaseResponse<T> implements Serializable {

    /**
     * 响应状态码
     */
    private int code;

    /**
     * 响应提示消息
     */
    private String message;

    /**
     * 响应业务数据
     */
    private T data;

    /**
     * 构造成功响应体
     *
     * @param codeBindMessageEnums 错误码与消息绑定枚举
     * @param data                 响应业务数据
     */
    public BaseResponse(CodeBindMessageEnums codeBindMessageEnums, T data) {
        this.code = codeBindMessageEnums.getCode();
        this.message = codeBindMessageEnums.getMessage();
        this.data = data;
    }

    /**
     * 构造错误响应体
     *
     * @param codeBindMessageEnums 错误码与消息绑定枚举
     * @param message              详细错误说明
     */
    public BaseResponse(CodeBindMessageEnums codeBindMessageEnums, String message) {
        this.code = codeBindMessageEnums.getCode();
        this.message = codeBindMessageEnums.getMessage() + ": " + message;
        this.data = null;
    }

    /**
     * 构造自定义响应体
     *
     * @param code    响应状态码
     * @param message 响应提示消息
     * @param data    响应业务数据
     */
    public BaseResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /// 序列化字段 ///

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

}