package cn.com.edtechhub.worktopicselection.manager.satoken;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.interceptor.SaInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;

/**
 * Sa-Token 权限认证配置类
 *
 * @author wobushi041
 */
@Configuration
@Slf4j
public class SaTokenConfig implements WebMvcConfigurer {

    /**
     * 构建并注册 SaInterceptor 路由拦截器以启用全局注解式鉴权
     *
     * @param registry Spring MVC 拦截器注册中心
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor()).addPathPatterns("/**");
    }

    /**
     * 容器初始化完成后输出 Sa-Token 核心配置与权限实现类日志
     */
    @PostConstruct
    public void printConfig() {
        log.debug("[SaTokenConfig] 当前项目 Sa-token 配置查验为: {}", SaManager.getConfig());
        log.debug("[SaTokenConfig] 当前项目 Sa-token 切面类被替换为: {}", SaManager.getStpInterface());
    }

}