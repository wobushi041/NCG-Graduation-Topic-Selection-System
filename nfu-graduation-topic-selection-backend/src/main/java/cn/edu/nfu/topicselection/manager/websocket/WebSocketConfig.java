package cn.edu.nfu.topicselection.manager.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * WebSocket 实时通信端点与跨域配置类
 *
 * @author wobushi041
 */
@Configuration
@EnableWebSocket // 启用 Spring WebSocket 支持
@Slf4j
public class WebSocketConfig implements WebSocketConfigurer {

    /**
     * WebSocket 全局消息服务端点访问路径
     */
    String path = "/global/message";

    /**
     * 注入 WebSocket 握手拦截器依赖
     */
    @Resource
    private WebSocketHandshakeInterceptor webSocketInterceptor;

    /**
     * 注入 WebSocket 消息处理器依赖
     */
    @Resource
    private WebSocketEditHandler webSocketEditHandler;

    /**
     * 允许建立 WebSocket 跨域连接的源地址列表配置
     */
    @Value("${app.websocket.allowed-origins}")
    private String allowedOrigins;

    /**
     * 注册 WebSocket 消息处理器、绑定握手拦截器并配置跨域访问规则
     *
     * @param registry WebSocket 处理器注册中心
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(webSocketEditHandler, this.path) // 把自定义的消息处理器 editHandler 注册到 path 这个端点
                .addInterceptors(webSocketInterceptor) // 给这个 WebSocket 通信加一个握手拦截器，用于连接前的校验或参数处理
                .setAllowedOrigins(this.getCorsRule()) // 允许所有来源跨域连接（生产环境中最好改成具体域名）
        ;
    }

    /**
     * 将逗号分隔的跨域源配置字符串解析为跨域源地址数组
     *
     * @return 允许跨域访问的源地址字符串数组
     */
    private String[] getCorsRule() {
        return StringUtils.commaDelimitedListToStringArray(allowedOrigins);
    }

    /**
     * 容器初始化完成后输出 WebSocket 端点路径与跨域规则配置日志
     */
    @PostConstruct
    public void printConfig() {
        Class<?> clazz = ClassUtils.getUserClass(this); // 获取原始类
        log.debug("[{}] path: {}", clazz.getSimpleName(), this.path);
        log.debug("[{}] getCorsRule: {}", clazz.getSimpleName(), this.getCorsRule());
    }

}
