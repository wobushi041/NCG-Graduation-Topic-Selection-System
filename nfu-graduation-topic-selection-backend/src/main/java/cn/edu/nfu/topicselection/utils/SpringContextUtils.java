package cn.edu.nfu.topicselection.utils;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring 应用上下文工具类
 *
 * @author wobushi041
 */
@Component
public class SpringContextUtils implements ApplicationContextAware {

    /**
     * Spring 应用上下文实例
     */
    private static ApplicationContext applicationContext;

    /**
     * 注入并保存 Spring 应用上下文实例
     *
     * @param applicationContext Spring 应用上下文对象
     */
    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
        SpringContextUtils.applicationContext = applicationContext;
    }

    /**
     * 根据名称获取 Spring 容器中的 Bean 实例
     *
     * @param beanName Bean 名称
     * @return 对应的 Bean 实例
     */
    public static Object getBean(String beanName) {
        return applicationContext.getBean(beanName);
    }

    /**
     * 根据类型获取 Spring 容器中的 Bean 实例
     *
     * @param beanClass Bean 类型
     * @param <T>       Bean 泛型类型
     * @return 对应的 Bean 实例
     */
    public static <T> T getBean(Class<T> beanClass) {
        return applicationContext.getBean(beanClass);
    }

    /**
     * 根据名称和类型获取 Spring 容器中的 Bean 实例
     *
     * @param beanName  Bean 名称
     * @param beanClass Bean 类型
     * @param <T>       Bean 泛型类型
     * @return 对应的 Bean 实例
     */
    public static <T> T getBean(String beanName, Class<T> beanClass) {
        return applicationContext.getBean(beanName, beanClass);
    }

}
