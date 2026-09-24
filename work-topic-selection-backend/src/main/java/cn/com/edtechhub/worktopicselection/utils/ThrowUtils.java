package cn.com.edtechhub.worktopicselection.utils;

import cn.com.edtechhub.worktopicselection.exception.BusinessException;
import cn.com.edtechhub.worktopicselection.exception.CodeBindMessageEnums;
import lombok.extern.slf4j.Slf4j;

/**
 * 业务异常抛出断言工具类
 *
 * @author wobushi041
 */
@Slf4j
public class ThrowUtils {

    /**
     * 条件成立时打印警告日志并抛出业务异常
     *
     * @param condition            触发抛出的布尔条件
     * @param codeBindMessageEnums 错误码与消息绑定枚举
     * @param message              异常详细消息
     */
    public static void throwIf(boolean condition, CodeBindMessageEnums codeBindMessageEnums, String message) {
        if (condition) {
            log.warn(message);
            throw new BusinessException(codeBindMessageEnums, message);
        }
    }

}
