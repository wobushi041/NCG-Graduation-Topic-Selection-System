package cn.edu.nfu.topicselection.aop;


import cn.hutool.core.lang.TypeReference;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 分页泛型类型引用构建工具类
 *
 * @author wobushi041
 */
public class TypeBuilder {

    /**
     * 构建带指定模型类型的 MyBatis-Plus 分页 TypeReference
     *
     * @param modelClass 分页记录的元素类型
     * @return 分页对象的 TypeReference 实例
     */
    public static TypeReference<Page<?>> buildPageTypeReference(Class<?> modelClass) {
        return new TypeReference<Page<?>>() {

            /**
             * 获取泛型参数化类型
             *
             * @return 参数化类型实例
             */
            @Override
            public Type getType() {
                return new ParameterizedType() {

                    /**
                     * 获取原始类型
                     *
                     * @return Page 原始类型
                     */
                    @Override
                    public Type getRawType() {
                        return Page.class;
                    }

                    /**
                     * 获取所有者类型
                     *
                     * @return 所有者类型（无则返回 null）
                     */
                    @Override
                    public Type getOwnerType() {
                        return null;
                    }

                    /**
                     * 获取实际泛型参数类型数组
                     *
                     * @return 实际泛型参数类型数组
                     */
                    @Override
                    public Type[] getActualTypeArguments() {
                        return new Type[]{modelClass};
                    }

                };
            }

        };
    }

}
