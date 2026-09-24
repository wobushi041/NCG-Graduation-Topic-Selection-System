package cn.com.edtechhub.worktopicselection.manager.mybatisplus;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * MyBatis-Plus 持久层框架配置类
 *
 * @author wobushi041
 */
@Configuration
@Slf4j
@MapperScan("cn.com.edtechhub.worktopicselection.mapper")
public class MyBatisPlusConfig {

    /**
     * 构建并注册集成 MySQL 分页插件的 MybatisPlusInterceptor 拦截器实例
     *
     * @return MyBatis-Plus 核心插件拦截器实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL)); // 分页插件
        return interceptor;
    }

    /**
     * 容器初始化完成后输出 MyBatis-Plus 分页插件配置状态日志
     */
    @PostConstruct
    public void printConfig() {
        log.debug("[MyBatisPlusConfig] 当前项目为 MyBatis Plus 配置了分页插件");
    }

}
