package cn.com.edtechhub.worktopicselection.aop;

import cn.com.edtechhub.worktopicselection.annotation.ValidateRequest;
import cn.com.edtechhub.worktopicselection.exception.BusinessException;
import cn.com.edtechhub.worktopicselection.exception.CodeBindMessageEnums;
import cn.com.edtechhub.worktopicselection.model.dto.auth.LoginRequest;
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

class RequestDtoValidationAspectTest {

    private RequestDtoValidationAspect aspect;
    private ProceedingJoinPoint joinPoint;
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

    @Test
    void rejectsNullRequestBodyBeforeController() {
        when(joinPoint.getArgs()).thenReturn(new Object[]{null});

        BusinessException exception = assertThrows(BusinessException.class,
                () -> aspect.around(joinPoint, validateRequest));

        assertEquals(CodeBindMessageEnums.PARAMS_ERROR, exception.getCodeBindMessageEnums());
        assertEquals("请求体不能为空", exception.getExceptionMessage());
    }

    @Test
    void returnsFirstViolationUsingStablePropertyOrder() {
        LoginRequest request = new LoginRequest();
        request.setAccount("");
        request.setPassword("");
        when(joinPoint.getArgs()).thenReturn(new Object[]{request});

        BusinessException exception = assertThrows(BusinessException.class,
                () -> aspect.around(joinPoint, validateRequest));

        assertEquals("账号不能为空", exception.getExceptionMessage());
    }

    @Test
    void proceedsWhenRequestBodyIsValid() throws Throwable {
        LoginRequest request = new LoginRequest();
        request.setAccount("20260001");
        request.setPassword("correct-password");
        when(joinPoint.getArgs()).thenReturn(new Object[]{request});
        when(joinPoint.proceed()).thenReturn("ok");

        assertEquals("ok", aspect.around(joinPoint, validateRequest));
        verify(joinPoint).proceed();
    }

    private static class ValidationTarget {

        @ValidateRequest
        public void handle(@RequestBody LoginRequest request) {
        }
    }
}
