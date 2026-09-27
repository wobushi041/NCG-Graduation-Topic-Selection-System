package cn.com.edtechhub.worktopicselection.manager.satoken;

import cn.dev33.satoken.aop.SaAroundAnnotationPointcutAdvisor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * 固定 Sa-Token 官方 AOP Advisor 的执行顺序
 *
 * @author wobushi041
 */
@Component
public class SaTokenAopOrderConfigurer implements BeanPostProcessor {

    /**
     * 在 Bean 初始化前设置 Sa-Token Advisor 的切面顺序
     *
     * @param bean     当前 Bean 实例
     * @param beanName 当前 Bean 名称
     * @return 处理后的 Bean 实例
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof SaAroundAnnotationPointcutAdvisor) {
            ((SaAroundAnnotationPointcutAdvisor) bean).setOrder(-200);
        }
        return bean;
    }

}
