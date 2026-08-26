package com.myproj.code.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.myproj.code.common.Result;
import com.myproj.code.entity.SessionLog;

import java.util.List;

public interface SessionLogService extends IService<SessionLog> {


    Result<?> readCtMessage(Integer sessionId, Integer userId);

    Result<?> readUserMessage(Integer sessionId, Integer ctId);

    Result<List<SessionLog>> getWindowMessage(Integer sessionId);

    Result<Integer> userGetUnreadMessageCount(Integer sessionId);

    Result<Integer> ctGetUnreadMessageCount(Integer sessionId);
}