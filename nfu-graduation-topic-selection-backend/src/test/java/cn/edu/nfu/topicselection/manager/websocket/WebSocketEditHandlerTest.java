package cn.edu.nfu.topicselection.manager.websocket;

import cn.edu.nfu.topicselection.model.entity.User;
import cn.edu.nfu.topicselection.manager.satoken.SaTokenManager;
import cn.edu.nfu.topicselection.model.enums.UserRoleEnum;
import cn.edu.nfu.topicselection.service.UserService;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WebSocket 编辑处理器单元测试
 *
 * @author wobushi041
 */
class WebSocketEditHandlerTest {

    /**
     * WebSocket 编辑处理器实例
     */
    private WebSocketEditHandler handler;

    /**
     * 用户服务 Mock 实例
     */
    private UserService userService;

    /**
     * Sa-Token 管理器 Mock 实例
     */
    private SaTokenManager saTokenManager;

    /**
     * 初始化测试依赖
     */
    @BeforeEach
    void setUp() {
        handler = new WebSocketEditHandler();
        userService = mock(UserService.class);
        saTokenManager = mock(SaTokenManager.class);
        ReflectionTestUtils.setField(handler, "userService", userService);
        ReflectionTestUtils.setField(handler, "saTokenManager", saTokenManager);
    }

    // 场景：测试非管理员发送消息时返回错误且不广播
    @Test
    void handleTextMessage_givenNonAdmin_sendsJsonErrorWithoutBroadcasting() throws Exception {
        User sender = user(1L);
        User receiver = user(2L);
        WebSocketSession senderSession = session(sender, 1L);
        WebSocketSession receiverSession = session(receiver, 1L);
        handler.afterConnectionEstablished(receiverSession);
        when(saTokenManager.isTokenValidForLoginId("token-1", sender.getId())).thenReturn(true);
        when(userService.getById(sender.getId())).thenReturn(sender);
        when(userService.userIsAdmin(sender)).thenReturn(false);

        handler.handleTextMessage(senderSession, validMessage());

        ArgumentCaptor<TextMessage> errorCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(senderSession).sendMessage(errorCaptor.capture());
        verify(receiverSession, never()).sendMessage(any(TextMessage.class));
        JSONObject error = JSONUtil.parseObj(errorCaptor.getValue().getPayload());
        assertEquals(WebSocketMessageTypeEnum.ERROR_MESSAGE.getCode(), error.getInt("typeCode"));
        assertEquals("权限认证错误: 仅管理员可以发送全局通知", error.getStr("message"));
    }

    // 场景：测试管理员发送消息时广播给已登录接收方
    @Test
    void handleTextMessage_givenAdmin_broadcastsToLoggedInReceiver() throws Exception {
        User sender = user(1L);
        User receiver = user(2L);
        WebSocketSession senderSession = session(sender, 1L);
        WebSocketSession receiverSession = session(receiver, 1L);
        handler.afterConnectionEstablished(receiverSession);
        when(userService.userIsAdmin(sender)).thenReturn(true);
        when(saTokenManager.isTokenValidForLoginId("token-1", sender.getId())).thenReturn(true);
        when(saTokenManager.isTokenValidForLoginId("token-2", receiver.getId())).thenReturn(true);
        when(userService.getById(sender.getId())).thenReturn(sender);
        when(userService.getById(receiver.getId())).thenReturn(receiver);
        TextMessage message = validMessage();

        handler.handleTextMessage(senderSession, message);

        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(receiverSession).sendMessage(messageCaptor.capture());
        verify(senderSession, never()).sendMessage(any(TextMessage.class));
        assertEquals(message.getPayload(), messageCaptor.getValue().getPayload());
    }

    // 场景：测试接收方已退出登录时关闭会话且不广播
    @Test
    void handleTextMessage_givenLoggedOutReceiver_closesWithoutBroadcasting() throws Exception {
        User sender = user(1L);
        User receiver = user(2L);
        WebSocketSession senderSession = session(sender, 1L);
        WebSocketSession receiverSession = session(receiver, 1L);
        handler.afterConnectionEstablished(receiverSession);
        when(userService.userIsAdmin(sender)).thenReturn(true);
        when(saTokenManager.isTokenValidForLoginId("token-1", sender.getId())).thenReturn(true);
        when(saTokenManager.isTokenValidForLoginId("token-2", receiver.getId())).thenReturn(false);
        when(userService.getById(sender.getId())).thenReturn(sender);

        handler.handleTextMessage(senderSession, validMessage());

        verify(receiverSession, never()).sendMessage(any(TextMessage.class));
        verify(receiverSession).close(CloseStatus.POLICY_VIOLATION);
    }

    /**
     * 构造测试用户实例
     *
     * @param id 用户 ID
     * @return 测试用户实例
     */
    private User user(long id) {
        User user = new User();
        user.setId(id);
        user.setUserName("user-" + id);
        user.setUserRole(UserRoleEnum.STUDENT.getCode());
        return user;
    }

    /**
     * 构造测试 WebSocket 会话
     *
     * @param user      关联用户
     * @param channelId 频道 ID
     * @return 测试 WebSocket 会话
     */
    private WebSocketSession session(User user, long channelId) {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("user", user);
        attributes.put("id", channelId);
        attributes.put("token", "token-" + user.getId());
        when(session.getAttributes()).thenReturn(attributes);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    /**
     * 构造合法的通知文本消息
     *
     * @return 文本消息实例
     */
    private TextMessage validMessage() {
        WebSocketMessage message = new WebSocketMessage();
        message.setTypeCode(WebSocketMessageTypeEnum.INFO_MESSAGE.getCode());
        message.setMessage("notice");
        return new TextMessage(JSONUtil.toJsonStr(message));
    }

}
