package cn.com.edtechhub.worktopicselection.aop;

import cn.com.edtechhub.worktopicselection.manager.satoken.SaTokenAopOrderConfigurer;
import cn.dev33.satoken.aop.SaAroundAnnotationPointcutAdvisor;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 认证模块 AOP 执行顺序测试
 *
 * @author wobushi041
 */
class AopOrderTest {

    // 场景：测试 Sentinel、Sa-Token 和 DTO 校验切面的执行顺序
    @Test
    void executesSentinelThenSaTokenThenDtoValidation() {
        // 1. 读取 Sentinel 和 DTO 校验切面的顺序配置
        Order sentinel = AnnotationUtils.findAnnotation(SentinelRateLimitAspect.class, Order.class);
        Order validation = AnnotationUtils.findAnnotation(RequestDtoValidationAspect.class, Order.class);

        // 2. 断言 Sentinel 和 DTO 校验切面的执行顺序
        assertNotNull(sentinel);
        assertNotNull(validation);
        assertEquals(-300, sentinel.value());
        assertEquals(-100, validation.value());

        // 3. 配置并断言 Sa-Token 官方 Advisor 的执行顺序
        SaAroundAnnotationPointcutAdvisor advisor = new SaAroundAnnotationPointcutAdvisor();
        new SaTokenAopOrderConfigurer().postProcessBeforeInitialization(advisor, "saTokenAdvisor");
        assertEquals(-200, advisor.getOrder());
    }

}
