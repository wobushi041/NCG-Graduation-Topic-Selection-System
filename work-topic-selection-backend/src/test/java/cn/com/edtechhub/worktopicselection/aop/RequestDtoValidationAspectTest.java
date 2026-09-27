package cn.com.edtechhub.worktopicselection.aop;

import cn.com.edtechhub.worktopicselection.annotation.ValidateRequest;
import cn.com.edtechhub.worktopicselection.exception.BusinessException;
import cn.com.edtechhub.worktopicselection.exception.CodeBindMessageEnums;
import cn.com.edtechhub.worktopicselection.model.request.auth.LoginRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.Validation;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 请求 DTO 校验切面测试
 *
 * @author wobushi041
 */
class RequestDtoValidationAspectTest {

    /**
     * 请求 DTO 校验切面
     */
    private RequestDtoValidationAspect aspect;

    /**
     * 控制器方法连接点
     */
    private ProceedingJoinPoint joinPoint;

    /**
     * 请求校验注解
     */
    private ValidateRequest validateRequest;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        aspect = new RequestDtoValidationAspect(
                Validation.buildDefaultValidatorFactory().getValidator()
        );
        Method method = ValidationTarget.class.getDeclaredMethod("handle", LoginRequest.class);
        validateRequest = method.getAnnotation(ValidateRequest.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(signature);
    }

    // 场景：测试空请求体在进入 Controller 前被拒绝
    @Test
    void rejectsNullRequestBodyBeforeController() {
        // 1. 准备空请求体参数
        when(joinPoint.getArgs()).thenReturn(new Object[]{null});

        // 2. 调用请求 DTO 校验切面
        BusinessException exception = assertThrows(BusinessException.class,
                () -> aspect.around(joinPoint, validateRequest));

        // 3. 断言返回统一参数错误
        assertEquals(CodeBindMessageEnums.PARAMS_ERROR, exception.getCodeBindMessageEnums());
        assertEquals("请求体不能为空", exception.getExceptionMessage());
    }

    // 场景：测试多个字段错误按属性名称稳定选择首个结果
    @Test
    void returnsFirstViolationUsingStablePropertyOrder() {
        // 1. 准备账号和密码均为空的登录请求
        LoginRequest request = new LoginRequest();
        request.setAccount("");
        request.setPassword("");
        when(joinPoint.getArgs()).thenReturn(new Object[]{request});

        // 2. 调用请求 DTO 校验切面
        BusinessException exception = assertThrows(BusinessException.class,
                () -> aspect.around(joinPoint, validateRequest));

        // 3. 断言优先返回账号字段错误
        assertEquals("账号不能为空", exception.getExceptionMessage());
    }

    // 场景：测试合法请求体继续执行 Controller 方法
    @Test
    void proceedsWhenRequestBodyIsValid() throws Throwable {
        // 1. 准备合法登录请求和连接点返回值
        LoginRequest request = new LoginRequest();
        request.setAccount("20260001");
        request.setPassword("correct-password");
        when(joinPoint.getArgs()).thenReturn(new Object[]{request});
        when(joinPoint.proceed()).thenReturn("ok");

        // 2. 调用请求 DTO 校验切面
        assertEquals("ok", aspect.around(joinPoint, validateRequest));

        // 3. 断言控制器连接点继续执行
        verify(joinPoint).proceed();
    }

    /**
     * 请求 DTO 校验切面测试目标
     *
     * @author wobushi041
     */
    private static class ValidationTarget {

        /**
         * 接收登录请求体
         *
         * @param request 登录请求
         */
        @ValidateRequest
        public void handle(@RequestBody LoginRequest request) {
        }

    }

}
