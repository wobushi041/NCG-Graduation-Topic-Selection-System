package cn.com.edtechhub.worktopicselection.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

/**
 * UTF-8 字节长度约束实现
 *
 * @author wobushi041
 */
public class Utf8ByteLengthValidator implements ConstraintValidator<Utf8ByteLength, String> {

    /**
     * 最小字节长度
     */
    private int min;

    /**
     * 最大字节长度
     */
    private int max;

    /**
     * 从约束注解读取字节长度上下限
     *
     * @param annotation UTF-8 字节长度约束注解
     */
    @Override
    public void initialize(Utf8ByteLength annotation) {
        min = annotation.min();
        max = annotation.max();
    }

    /**
     * 校验字符串的 UTF-8 字节长度是否位于约束区间
     *
     * @param value   待校验字符串
     * @param context 校验上下文
     * @return 字符串是否满足字节长度约束
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        int length = value.getBytes(StandardCharsets.UTF_8).length;
        return length >= min && length <= max;
    }

}
