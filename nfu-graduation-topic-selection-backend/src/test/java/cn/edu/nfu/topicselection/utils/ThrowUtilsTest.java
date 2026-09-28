package cn.edu.nfu.topicselection.utils;

import cn.edu.nfu.topicselection.exception.BusinessException;
import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ThrowUtils 断言工具类单元测试
 *
 * @author wobushi041
 */
class ThrowUtilsTest {

    /**
     * 测试用错误码枚举
     */
    private static final CodeBindMessageEnums TEST_ENUM = CodeBindMessageEnums.PARAMS_ERROR;

    /**
     * 测试用错误消息
     */
    private static final String TEST_MESSAGE = "参数错误";

    /// 测试 throwIf 方法 ///

    // 场景：测试条件为 false 时不抛出异常
    @Test
    void throwIf_conditionFalse_doesNothing() {
        // 直接执行，不抛异常就通过
        assertDoesNotThrow(() -> ThrowUtils.throwIf(false, TEST_ENUM, TEST_MESSAGE));
    }

    // 场景：测试条件为 true 时抛出对应的 BusinessException
    @Test
    void throwIf_conditionTrue_throwsBusinessException() {
        // 捕获异常并验证
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            ThrowUtils.throwIf(true, TEST_ENUM, TEST_MESSAGE);
        });

        // 验证枚举和消息
        assertEquals(TEST_ENUM, exception.getCodeBindMessageEnums());
        assertEquals(TEST_MESSAGE, exception.getMessage());
    }

    // 场景：测试消息为 null 且条件为 true 时正常抛出异常
    @Test
    void throwIf_messageNull_throwsException() {
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            ThrowUtils.throwIf(true, TEST_ENUM, null);
        });
        assertNull(exception.getMessage());
    }

}
