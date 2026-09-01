package com.myproj.code.ai.service;

import com.myproj.code.entity.Session;
import com.myproj.code.websocket.endpoint.UserServiceEndpoint;
import com.myproj.code.websocket.message.ChatMessage;

public interface AIService {
    public Boolean turnToMunalJudgement(Session chatSession, ChatMessage chatMessage);

    public void chat(Session chatSession, ChatMessage chatMessage, UserServiceEndpoint userServiceEndpoint) throws IllegalAccessException;
}
