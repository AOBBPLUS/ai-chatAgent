package com.myproj.code.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.myproj.code.common.Result;
import com.myproj.code.entity.SessionLog;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

public interface SessionLogService extends IService<SessionLog> {


    Result<?> readCtMessage(Integer sessionId, Integer userId);

    Result<?> readUserMessage(Integer sessionId, Integer ctId);

    Result<List<SessionLog>> getWindowMessage(Integer sessionId);

    Result<Integer> userGetUnreadMessageCount(Integer sessionId);

    Result<Integer> ctGetUnreadMessageCount(Integer sessionId);

    List<SessionLog> tryGetSessionLogs(String conversationId);

    void addToRedis(String conversationId, List<SessionLog> sessionLogs);

    void add(String conversationId, List<Message> messages);
}