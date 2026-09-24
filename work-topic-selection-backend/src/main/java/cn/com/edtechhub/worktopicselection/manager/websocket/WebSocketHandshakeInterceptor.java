package cn.com.edtechhub.worktopicselection.manager.websocket;

import cn.com.edtechhub.worktopicselection.model.entity.User;
import cn.com.edtechhub.worktopicselection.model.enums.UserRoleEnum;
import cn.com.edtechhub.worktopicselection.service.UserService;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * WebSocket 握手阶段身份鉴权与上下文属性注入拦截器
 *
 * @author wobushi041
 */
@Component
@Slf4j
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    /**
     * 注入用户服务依赖
     */
    @Resource
    UserService userService;

    /**
     * 在 WebSocket 协议升级握手前校验用户登录态与频道参数并写入会话属性
     *
     * @param request    当前握手的 HTTP 服务端请求对象
     * @param response   当前握手的 HTTP 服务端响应对象
     * @param wsHandler  目标 WebSocket 消息处理器
     * @param attributes 传递给 WebSocketSession 的上下文属性映射表
     * @return 校验通过允许握手时返回 true，拒绝握手时返回 false
     */
    @Override
    public boolean beforeHandshake(@NotNull ServerHttpRequest request, @NotNull ServerHttpResponse response, @NotNull WebSocketHandler wsHandler, @NotNull Map<String, Object> attributes) {
        log.debug("WebSocket 握手开始前触发 beforeHandshake(), 其中 {} {} {}", request, response, wsHandler);

        // 如果当前请求基于 Servlet 的 HTTP 请求
        if (!(request instanceof ServletServerHttpRequest)) {
            return false;
        }

        // 获取网络请求对象
        HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

        // 设置 userVO 参数
        User sessionUser = userService.userGetCurrentLoginUser();
        User user = sessionUser == null || sessionUser.getId() == null
                ? null
                : userService.getById(sessionUser.getId());
        UserRoleEnum role = user == null || user.getUserRole() == null
                ? null
                : UserRoleEnum.getEnums(user.getUserRole());
        if (user == null || role == null || role == UserRoleEnum.BAN_ROLE) {
            log.error("用户尚未登录, 拒绝握手");
            return false;
        }
        attributes.put("user", user);
        String tokenValue = StpUtil.getTokenValue();
        if (tokenValue == null || tokenValue.trim().isEmpty()) {
            return false;
        }
        attributes.put("token", tokenValue);

        // 设置 id 参数
        String idStr = servletRequest.getParameter("id");
        if (idStr == null || idStr.isEmpty()) {
            return false;
        }
        try {
            Long id = Long.parseLong(idStr);
            if (!Long.valueOf(1L).equals(id)) {
                return false;
            }
            attributes.put("id", id);
        } catch (NumberFormatException e) {
            log.debug("无效的 id 参数: {}", idStr);
            return false;
        }

        // 允许握手
        return true;
    }

    /**
     * 在 WebSocket 协议升级握手完成后记录调试日志
     *
     * @param request   当前握手的 HTTP 服务端请求对象
     * @param response  当前握手的 HTTP 服务端响应对象
     * @param wsHandler 目标 WebSocket 消息处理器
     * @param exception 握手过程中产生的异常对象（无异常时为 null）
     */
    @Override
    public void afterHandshake(@NotNull ServerHttpRequest request, @NotNull ServerHttpResponse response, @NotNull WebSocketHandler wsHandler, Exception exception) {
        log.debug("WebSocket 握手结束后触发 afterHandshake(), 其中 {} {} {}", request, response, wsHandler);
    }

}
