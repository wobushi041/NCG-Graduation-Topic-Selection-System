package cn.com.edtechhub.worktopicselection.aop;

import cn.com.edtechhub.worktopicselection.annotation.ValidateRequest;
import cn.com.edtechhub.worktopicselection.exception.BusinessException;
import cn.com.edtechhub.worktopicselection.exception.CodeBindMessageEnums;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Set;

/**
 * 在控制器执行前统一校验 RequestBody DTO。
 */
@Aspect
@Component
@Order(-100)
public class RequestDtoValidationAspect {

    private final Validator validator;

    public RequestDtoValidationAspect(Validator validator) {
        this.validator = validator;
    }

    @Around("@annotation(validateRequest)")
    public Object around(ProceedingJoinPoint joinPoint, ValidateRequest validateRequest) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Object[] arguments = joinPoint.getArgs();
        for (int index = 0; index < arguments.length; index++) {
            if (!hasRequestBody(parameterAnnotations[index])) {
                continue;
            }
            Object argument = arguments[index];
            if (argument == null) {
                throw new BusinessException(CodeBindMessageEnums.PARAMS_ERROR, "请求体不能为空");
            }
            validate(argument);
        }
        return joinPoint.proceed();
    }

    private void validate(Object argument) {
        Set<ConstraintViolation<Object>> violations = validator.validate(argument);
        violations.stream()
                .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                .findFirst()
                .ifPresent(violation -> {
                    throw new BusinessException(CodeBindMessageEnums.PARAMS_ERROR, violation.getMessage());
                });
    }

    private boolean hasRequestBody(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType() == RequestBody.class) {
                return true;
            }
        }
        return false;
    }
}
