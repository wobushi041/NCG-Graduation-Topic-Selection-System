package cn.com.edtechhub.worktopicselection.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 校验字符串 UTF-8 编码后的字节长度
 *
 * @author wobushi041
 */
@Documented
@Constraint(validatedBy = Utf8ByteLengthValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Utf8ByteLength {

    /**
     * 获取校验失败提示信息
     *
     * @return 校验失败提示信息
     */
    String message() default "字段字节长度不正确";

    /**
     * 获取允许的最小字节长度
     *
     * @return 最小字节长度
     */
    int min() default 0;

    /**
     * 获取允许的最大字节长度
     *
     * @return 最大字节长度
     */
    int max() default Integer.MAX_VALUE;

    /**
     * 获取 Bean Validation 校验分组
     *
     * @return 校验分组
     */
    Class<?>[] groups() default {};

    /**
     * 获取 Bean Validation 负载类型
     *
     * @return 负载类型
     */
    Class<? extends Payload>[] payload() default {};

}
