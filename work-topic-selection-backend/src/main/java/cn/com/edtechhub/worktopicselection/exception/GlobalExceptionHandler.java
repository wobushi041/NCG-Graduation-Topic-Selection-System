package cn.com.edtechhub.worktopicselection.exception;

import cn.com.edtechhub.worktopicselection.response.BaseResponse;
import cn.com.edtechhub.worktopicselection.response.TheResult;
import cn.dev33.satoken.exception.DisableServiceException;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

/**
 * 全局异常处理器
 *
 * @author wobushi041
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 兜底拦截并处理全局未知异常
     *
     * @param e 捕获到的异常实例
     * @return 系统错误响应体
     */
    @ExceptionHandler
    public BaseResponse<String> exceptionHandler(Exception e) {
        log.error("触发全局所有异常处理方法");
        printStackTraceStatus(e, 0);
        return TheResult.error(CodeBindMessageEnums.SYSTEM_ERROR, "请联系系统管理员");
    }

    /**
     * 拦截并处理业务自定义异常
     *
     * @param e 业务异常实例
     * @return 业务错误响应体
     */
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.warn("触发业务内部异常处理方法");
        printStackTraceStatus(e, 1);
        return TheResult.error(e.getCodeBindMessageEnums(), e.exceptionMessage);
    }

    /**
     * 拦截并处理 Sa-Token 登录认证异常
     *
     * @return 未登录错误响应体
     */
    @ExceptionHandler(NotLoginException.class)
    public BaseResponse<?> notLoginExceptionHandler() {
        log.warn("触发登录认证异常处理方法");
        return TheResult.error(CodeBindMessageEnums.NO_LOGIN_ERROR, "请先进行登录");
    }

    /**
     * 拦截并处理 Sa-Token 角色标识认证异常
     *
     * @return 角色认证错误响应体
     */
    @ExceptionHandler(NotRoleException.class)
    public BaseResponse<?> notRoleExceptionHandler() {
        log.warn("触发权限认证异常处理方法(角色标识认证)");
        return TheResult.error(CodeBindMessageEnums.NO_ROLE_ERROR, "用户当前角色不允许使用该功能");
    }

    /**
     * 拦截并处理 Sa-Token 权限码值认证异常
     *
     * @return 权限认证错误响应体
     */
    @ExceptionHandler(NotPermissionException.class)
    public BaseResponse<?> notPermissionExceptionHandler() {
        log.warn("触发权限认证异常处理方法(权限码值认证)");
        return TheResult.error(CodeBindMessageEnums.NO_AUTH_ERROR, "用户当前权限不允许使用该功能, 请申请权限");
    }

    /**
     * 拦截并处理 Sa-Token 账号封禁异常
     *
     * @return 账号封禁错误响应体
     */
    @ExceptionHandler(DisableServiceException.class)
    public BaseResponse<?> disableServiceExceptionHandler() {
        log.warn("触发用户封禁异常处理方法");
        return TheResult.error(CodeBindMessageEnums.USER_DISABLE_ERROR, "当前用户因为违规被封禁");
    }

    /**
     * 拦截并处理 Sentinel 流量控制异常
     *
     * @return 流量控制错误响应体
     */
    @ExceptionHandler(BlockException.class)
    public BaseResponse<?> handleBlockExceptionHandler() {
        log.warn("触发流量控制异常处理方法");
        return TheResult.error(CodeBindMessageEnums.FLOW_RULES, "请求流量过大, 请稍后再试");
    }

    /**
     * 拦截并处理 Hibernate Validator 参数校验异常
     *
     * @param ex 方法参数校验异常实例
     * @return 参数错误响应体
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<?> handleValidationException(MethodArgumentNotValidException ex) {
        // 获取第一个字段错误
        String errorMessage = ex
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map((error) -> {
                    log.warn("{}", error.getField() + " - " + error.getDefaultMessage());
                    return error.getDefaultMessage();
                })
                .orElse("请求参数校验失败"); // 兜底提示
        return TheResult.error(CodeBindMessageEnums.PARAMS_ERROR, errorMessage);
    }

    /**
     * 将 JSON 语法错误和字段类型转换失败包装为统一参数错误响应
     *
     * @param ex HTTP 消息不可读异常
     * @return 统一参数错误响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public BaseResponse<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("请求 JSON 无法解析: {}", ex.getMessage());
        return TheResult.error(CodeBindMessageEnums.PARAMS_ERROR, "请求体格式不正确");
    }

    /**
     * 打印指定堆栈层级的异常代码定位日志
     *
     * @param e    异常实例
     * @param tier 堆栈元素层级索引
     */
    private void printStackTraceStatus(Exception e, int tier) {
        StackTraceElement element = e.getStackTrace()[tier];
        // 获取异常抛出位置（第一个堆栈元素）
        log.warn("异常位置: {} -> 文件: {}, 方法: {}, 码行: {}",
                element.getFileName(),
                element.getClassName(),
                element.getMethodName(),
                element.getLineNumber()
        );
    }

}
