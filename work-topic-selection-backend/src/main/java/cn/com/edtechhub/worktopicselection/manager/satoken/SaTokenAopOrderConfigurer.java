package cn.com.edtechhub.worktopicselection.manager.satoken;

import cn.dev33.satoken.aop.SaAroundAnnotationPointcutAdvisor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * 固定 Sa-Token 官方 AOP Advisor 的执行顺序。
 */
@Component
public class SaTokenAopOrderConfigurer implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof SaAroundAnnotationPointcutAdvisor) {
            ((SaAroundAnnotationPointcutAdvisor) bean).setOrder(-200);
        }
        return bean;
    }
}
