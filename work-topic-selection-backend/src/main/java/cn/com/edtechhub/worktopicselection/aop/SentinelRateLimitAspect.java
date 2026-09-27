package cn.com.edtechhub.worktopicselection.aop;

import cn.com.edtechhub.worktopicselection.annotation.SentinelRateLimit;
import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 使用 Sentinel 包围完整控制器方法的限流切面。
 */
@Aspect
@Component
@Order(-300)
public class SentinelRateLimitAspect {

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, SentinelRateLimit rateLimit) throws Throwable {
        Entry entry = null;
        try {
            entry = SphU.entry(rateLimit.resource());
            return joinPoint.proceed();
        } catch (BlockException exception) {
            throw exception;
        } catch (Throwable throwable) {
            Tracer.trace(throwable);
            throw throwable;
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }
}
