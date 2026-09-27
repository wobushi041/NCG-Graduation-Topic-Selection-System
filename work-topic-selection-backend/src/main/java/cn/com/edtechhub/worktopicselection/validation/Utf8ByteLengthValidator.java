package cn.com.edtechhub.worktopicselection.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

/**
 * UTF-8 字节长度约束实现。
 */
public class Utf8ByteLengthValidator implements ConstraintValidator<Utf8ByteLength, String> {

    private int min;
    private int max;

    @Override
    public void initialize(Utf8ByteLength annotation) {
        min = annotation.min();
        max = annotation.max();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        int length = value.getBytes(StandardCharsets.UTF_8).length;
        return length >= min && length <= max;
    }
}
