package cn.com.edtechhub.worktopicselection.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import javax.annotation.PostConstruct;

/**
 * Spring MVC JSON 序列化配置类
 *
 * @author wobushi041
 */
@JsonComponent
@Slf4j
public class JsonConfig {

    /**
     * 构建并注册自定义 Long 转字符串序列化的 ObjectMapper 实例，防止前端长整型精度丢失
     *
     * @param builder Jackson2ObjectMapper 构建器
     * @return 自定义配置的 ObjectMapper 实例
     */
    @Bean
    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        objectMapper.registerModule(module);
        return objectMapper;
    }

    /**
     * 初始化完成后打印 JSON 序列化精度配置日志
     */
    @PostConstruct
    public void printConfig() {
        log.debug("[JsonConfig] 当前项目自动解决了 Long 在前后端传递过程中的精度问题");
    }

}
