package cn.com.edtechhub.worktopicselection.response;

import cn.com.edtechhub.worktopicselection.exception.CodeBindMessageEnums;

/**
 * 通用响应体快捷构建工具类
 *
 * @author wobushi041
 */
public class TheResult {

    /**
     * 构造成功响应体
     *
     * @param codeBindMessageEnums 错误码与消息绑定枚举
     * @param data                 响应业务数据
     * @param <T>                  响应数据类型
     * @return 成功响应体
     */
    public static <T> BaseResponse<T> success(CodeBindMessageEnums codeBindMessageEnums, T data) {
        return new BaseResponse<>(codeBindMessageEnums, data);
    }

    /**
     * 构造失败响应体
     *
     * @param codeBindMessageEnums 错误码与消息绑定枚举
     * @param message              详细错误消息
     * @param <T>                  响应数据类型
     * @return 失败响应体
     */
    public static <T> BaseResponse<T> error(CodeBindMessageEnums codeBindMessageEnums, String message) {
        return new BaseResponse<>(codeBindMessageEnums, message);
    }

    /**
     * 构造接口待开发响应体
     *
     * @param <T> 响应数据类型
     * @return 待开发响应体
     */
    public static <T> BaseResponse<T> notyet() {
        return new BaseResponse<>(-1, "该接口尚在开发中", null);
    }

    /**
     * 构造带附加说明的接口待开发响应体
     *
     * @param test 附加说明信息
     * @param <T>  响应数据类型
     * @return 待开发响应体
     */
    public static <T> BaseResponse<T> notyet(String test) {
        return new BaseResponse<>(-1, "该接口尚在开发中: " + test, null);
    }

}
