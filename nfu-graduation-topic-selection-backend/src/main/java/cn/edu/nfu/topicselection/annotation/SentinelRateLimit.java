package cn.edu.nfu.topicselection.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明由 Sentinel 保护的方法资源
 *
 * @author wobushi041
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)//编译后保留到 JVM 运行时，切面才能在运行时通过反射读到
public @interface SentinelRateLimit {

    /**
     * Sentinel 资源名称
     *
     * @return Sentinel 资源名称
     */
    String resource();

}
