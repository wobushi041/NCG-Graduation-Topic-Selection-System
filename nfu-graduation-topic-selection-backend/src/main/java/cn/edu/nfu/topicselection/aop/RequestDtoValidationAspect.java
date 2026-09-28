package cn.edu.nfu.topicselection.aop;

import cn.edu.nfu.topicselection.annotation.ValidateRequest;
import cn.edu.nfu.topicselection.exception.BusinessException;
import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
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
 * 在 Controller 执行前统一校验 RequestBody DTO
 *
 * @author wobushi041
 */
@Aspect
@Component
@Order(-100)
public class RequestDtoValidationAspect {

    /**
     * 注入 Bean Validation 校验器依赖
     */
    private final Validator validator;

    /**
     * 初始化请求 DTO 校验切面
     *
     * @param validator Bean Validation 校验器
     */
    public RequestDtoValidationAspect(Validator validator) {
        this.validator = validator;
    }

    /// 请求校验 ///

    /**
     * 校验控制器方法中的请求体参数并继续执行调用链
     *
     * @param joinPoint       控制器方法连接点
     * @param validateRequest 请求校验注解
     * @return 控制器方法执行结果
     */
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

    /**
     * 执行请求对象的字段约束校验
     *
     * @param argument 请求对象
     */
    private void validate(Object argument) {
        Set<ConstraintViolation<Object>> violations = validator.validate(argument);
        violations.stream()
                .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                .findFirst()
                .ifPresent(violation -> {
                    throw new BusinessException(CodeBindMessageEnums.PARAMS_ERROR, violation.getMessage());
                });
    }

    /**
     * 判断参数是否标记为请求体
     *
     * @param annotations 参数注解集合
     * @return 是否为请求体参数
     */
    private boolean hasRequestBody(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType() == RequestBody.class) {
                return true;
            }
        }
        return false;
    }

}
