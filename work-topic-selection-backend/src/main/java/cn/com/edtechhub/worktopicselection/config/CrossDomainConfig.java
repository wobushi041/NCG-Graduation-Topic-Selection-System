package cn.com.edtechhub.worktopicselection.config;

import cn.com.edtechhub.worktopicselection.interceptor.RequestLoggingInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;

/**
 * 全局跨域与拦截器配置类
 *
 * @author wobushi041
 */
@Configuration
@Slf4j
public class CrossDomainConfig implements WebMvcConfigurer {

    /**
     * 注入请求日志拦截器依赖
     */
    @Resource
    private RequestLoggingInterceptor requestLoggingInterceptor;

    /**
     * 允许跨域的源地址配置
     */
    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * 注册全局请求日志拦截器并配置拦截路径规则
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestLoggingInterceptor).addPathPatterns("/**");
    }

    /**
     * 配置全局跨域资源共享映射策略
     *
     * @param registry 跨域注册表
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry
                .addMapping("/**")
                .allowedOriginPatterns(this.getCorsRule().toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 解析并获取允许跨域的源地址列表
     *
     * @return 允许跨域的源地址列表
     */
    private List<String> getCorsRule() {
        return java.util.Arrays.asList(StringUtils.commaDelimitedListToStringArray(allowedOrigins));
    }

    /**
     * 初始化完成后打印当前跨域规则配置日志
     */
    @PostConstruct
    public void printConfig() {
        log.debug("[CrossDomainConfig] 当前项目 Cors 跨域规则为 {}", this.getCorsRule());
    }

}
