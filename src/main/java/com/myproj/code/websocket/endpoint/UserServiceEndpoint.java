package com.myproj.code.websocket.endpoint;

import com.myproj.code.config.ChatMessageCoder;
import com.myproj.code.mapper.SessionLogMapper;
import com.myproj.code.service.SessionService;
import com.myproj.code.websocket.message.ChatMessage;
import com.myproj.code.entity.SessionLog;
import jakarta.annotation.Resource;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ServerEndpoint(value = "/user/chat/{userId}", decoders = ChatMessageCoder.class, encoders = ChatMessageCoder.class)
public class UserServiceEndpoint implements WebSocketEndpoint {

    private static SessionService sessionService;

    // 存储用户会话
    private static final ConcurrentHashMap<Integer, UserServiceEndpoint> userEndpointPool = new ConcurrentHashMap<>();

    private Session session;
    private Integer userId;
    @Resource
    private SessionLogMapper sessionLogMapper;

    @Autowired
    public void setSessionService(SessionService sessionService) {
        UserServiceEndpoint.sessionService = sessionService;
    }

    /**
     * 建立连接触发
     * @param session
     * @param userId
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Integer userId) {
        this.session = session;
        this.userId = userId;
        userEndpointPool.put(userId, this);
    }

    /**
     * 关闭触发
     */
    @OnClose
    public void onClose() {
        if (userId != null) {
            userEndpointPool.remove(userId);
        }
    }

    /**
     * 接收到消息时的处理方法
     * 前端使用想商家发送消息
     */
    // TODO:005
    @OnMessage
    public void onMessage(ChatMessage message, Session session) throws EncodeException, IOException {
        // 设置消息类型
        message.setType(SessionLog.Type.USER);
        // 查找
        com.myproj.code.entity.Session session1 = sessionService.find(message, userId,this);
        switch (session1.getConversationStatus()){
            case AI -> {}
            case HUMAN -> {
                // 插入聊天记录
                sessionLogMapper.insert(SessionLog.builder()
                                .sessionId(message.getSessionId())
                                .content(message.getMessage())
                                .type(message.getType())
                        .build());
                // 获取商户端点
            }
        }
    }

    /**
     * 发生错误触发
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        try {
            error.printStackTrace();
            session.getBasicRemote().sendObject(ChatMessage.builder()
                    .state(ChatMessage.State.ERROR)
                    .message(error.getMessage())
                    .build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 封装发送消息的方法
     */
    public void sendMessage(ChatMessage chatMessage) throws EncodeException, IOException {
        this.session.getBasicRemote().sendObject(chatMessage);
    }

    @Override
    public SessionLog.Type getEndpointType() {
        return SessionLog.Type.USER;
    }

    public static UserServiceEndpoint findEndPoint(Integer userId) {
        return userEndpointPool.get(userId);
    }

    @Autowired
    public void setSessionLogMapper(SessionLogMapper sessionLogMapper) {
        this.sessionLogMapper = sessionLogMapper;
    }
}
