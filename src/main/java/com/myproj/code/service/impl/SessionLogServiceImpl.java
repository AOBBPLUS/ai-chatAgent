package com.myproj.code.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myproj.code.service.SessionLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myproj.code.code.ResultCode;
import com.myproj.code.common.Result;
import com.myproj.code.entity.Session;
import com.myproj.code.entity.SessionLog;
import com.myproj.code.mapper.SessionLogMapper;
import com.myproj.code.mapper.SessionMapper;
import com.myproj.code.utils.KeyUtils;
import com.myproj.code.utils.TypeConversion;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionLogServiceImpl extends ServiceImpl<SessionLogMapper, SessionLog> implements SessionLogService {
    private final StringRedisTemplate stringRedisTemplate;
    private final SessionMapper sessionMapper;
    private final SessionLogMapper sessionLogMapper;
    @Value("${memory.redis-length}")
    private Integer memoryLength;
    @Value("${memory.expiration-duration}")
    private Integer timeout;
    @Value("${memory.key}")
    private String memoryKey;


    @Override
    public Result<?> readCtMessage(Integer sessionId, Integer userId) {
        // 检查是否有该会话
        Session session = sessionMapper.selectOne(new LambdaQueryWrapper<>(Session.class)
                .eq(Session::getId, sessionId)
                .eq(Session::getUserId, userId)
        );
        if (session == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        // 修改这个session的关于商户或者AI发送的消息
        lambdaUpdate().set(SessionLog::getReadStatus, SessionLog.ReadStatus.READ)
                .eq(SessionLog::getSessionId, sessionId)
                .eq(SessionLog::getType, SessionLog.Type.ASSISTANT)
                .or()
                .eq(SessionLog::getType, SessionLog.Type.COMMERCIAL_TENANT)
                .update();
        return Result.success(ResultCode.UPDATE_SUCCESS);
    }

    @Override
    public Result<?> readUserMessage(Integer sessionId, Integer ctId) {
        // 检查是否有该会话
        Session session = sessionMapper.selectOne(new LambdaQueryWrapper<>(Session.class)
                .eq(Session::getId, sessionId)
                .eq(Session::getCtId, ctId)
        );
        if (session == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        // 修改这个session的关于商户或者AI发送的消息
        lambdaUpdate().set(SessionLog::getReadStatus, SessionLog.ReadStatus.READ)
                .eq(SessionLog::getSessionId, sessionId)
                .eq(SessionLog::getType, SessionLog.Type.USER)
                .update();
        return Result.success(ResultCode.UPDATE_SUCCESS);
    }

    @Override
    public Result<List<SessionLog>> getWindowMessage(Integer sessionId) {
        return Result.success(
                ResultCode.GET_SUCCESS,
                lambdaQuery().eq(SessionLog::getSessionId, sessionId)
                        .orderByAsc(SessionLog::getTimestamp)
                        .list()
        );
    }

    @Override
    public Result<Integer> userGetUnreadMessageCount(Integer sessionId) {
        return Result.success(
                ResultCode.GET_SUCCESS,
                lambdaQuery().eq(SessionLog::getSessionId, sessionId)
                        .eq(SessionLog::getType, SessionLog.Type.COMMERCIAL_TENANT)
                        .or()
                        .eq(SessionLog::getType, SessionLog.Type.ASSISTANT)
                        .count().intValue()
        );
    }

    @Override
    public Result<Integer> ctGetUnreadMessageCount(Integer sessionId) {
        return Result.success(
                ResultCode.GET_SUCCESS,
                lambdaQuery().eq(SessionLog::getSessionId, sessionId)
                        .eq(SessionLog::getType, SessionLog.Type.USER)
                        .count().intValue()
        );
    }

    @Override
    public List<SessionLog> tryGetSessionLogs(String conversationId) {
        // 分页查询前10条数据
        return lambdaQuery().eq(SessionLog::getId, conversationId)
                .orderByDesc(SessionLog::getTimestamp)
                .page(new Page<>(1, memoryLength))
                .getRecords();
    }

    @Override
    public void addToRedis(String conversationId, List<SessionLog> sessionLogs) {
        if (sessionLogs.isEmpty()) {
            return;
        }
        String redisKey = KeyUtils.redisKeyUtils(memoryKey, conversationId);
        // 添加Redis中、
        stringRedisTemplate.opsForList().rightPushAll(
                redisKey,
                sessionLogs.stream().map(JSONUtil::toJsonStr).toList()
        );
        // 重置过期时间
        stringRedisTemplate.expire(redisKey, timeout, TimeUnit.MINUTES);
        // 判断长度是否超过限制，超过删除
        if (Optional.ofNullable(stringRedisTemplate.opsForList().size(redisKey)).orElse(0L) > memoryLength) {
            stringRedisTemplate.opsForList().trim(redisKey, -memoryLength, -1);
        }

    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        // 添加到mysql
        List<SessionLog> sessionLogCollection = messages.stream().map(message -> {
            SessionLog.Type type = TypeConversion.messageToSessionType(message.getMessageType());
            return SessionLog.builder()
                    .sessionId(Integer.valueOf(conversationId))
                    .content(message.getText())
                    .type(type)
                    .build();
        }).toList();
        // 批量添加数据库
        sessionLogMapper.insert(sessionLogCollection);
        // 批量添加Redis
        addToRedis(conversationId,sessionLogCollection);

    }
}