package cn.edu.nfu.topicselection.annotation;

import java.lang.annotation.*;

/**
 * 查询接口远端缓存优化注解
 *
 * @author wobushi041
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheSearchOptimization {

    /**
     * 默认缓存过期时间（单位：秒）
     */
    long ttl() default 30;

    /**
     * 分页数据实体类型
     */
    Class<?> modelClass();

}
