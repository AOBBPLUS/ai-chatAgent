package com.myproj.code.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.myproj.code.common.Result;
import com.myproj.code.entity.Session;
import com.myproj.code.entity.SessionLog;
import com.myproj.code.websocket.endpoint.UserServiceEndpoint;
import com.myproj.code.websocket.message.ChatMessage;
import jakarta.websocket.EncodeException;

import java.io.IOException;
import java.util.List;

public interface SessionService extends IService<Session> {

    Result<List<Session>> userGetLastSessionList(Integer userId);

    Result<List<Session>> ctGetLastSessionList(Integer ctId);

    Session find(ChatMessage message, Integer userId, UserServiceEndpoint userServiceEndpoint) throws EncodeException, IOException;

}