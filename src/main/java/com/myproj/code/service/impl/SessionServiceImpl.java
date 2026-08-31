package com.myproj.code.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.myproj.code.code.ResultCode;
import com.myproj.code.common.Result;
import com.myproj.code.entity.Role;
import com.myproj.code.entity.Session;
import com.myproj.code.mapper.SessionMapper;
import com.myproj.code.service.RoleService;
import com.myproj.code.service.SessionService;
import com.myproj.code.utils.KeyUtils;
import com.myproj.code.utils.SessionFind;
import com.myproj.code.websocket.endpoint.UserServiceEndpoint;
import com.myproj.code.websocket.message.ChatMessage;
import jakarta.annotation.Resource;
import jakarta.websocket.EncodeException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisKeyValueTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl extends ServiceImpl<SessionMapper, Session> implements SessionService {
    @Value("${session.key}")
    private String key;
    @Value("${session.expiration-duration}")
    private Integer timeout;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RoleService roleService;

    @Resource
    private SessionFind sessionFind;


    @Override
    public Result<List<Session>> userGetLastSessionList(Integer userId) {
        return Result.success(
                ResultCode.GET_SUCCESS,
                lambdaQuery()
                        .eq(Session::getUserId, userId)
                        .orderByDesc(Session::getTimestamp)
                        .list()
        );
    }

    @Override
    public Result<List<Session>> ctGetLastSessionList(Integer ctId) {
        return Result.success(
                ResultCode.GET_SUCCESS,
                lambdaQuery()
                        .eq(Session::getCtId, ctId)
                        .orderByDesc(Session::getTimestamp)
                        .list()
        );
    }

    @Override
    public Session find(ChatMessage message, Integer userId, UserServiceEndpoint userServiceEndpoint) throws EncodeException, IOException {
        //判断是否为新增的会话
        if (!ObjectUtils.isEmpty(message.getSessionId())) {
            return sessionFind.getSessionById(message.getSessionId());
        }
        if (ObjectUtils.isEmpty(message.getCtId()) || ObjectUtils.isEmpty(message.getGoodsId())) {
            throw new IllegalArgumentException("参数缺失");
        }
        Session session = Session.builder()
                .ctId(message.getCtId())
                .goodsId(message.getGoodsId())
                .userId(userId)
                .build();
        // 查询是否设置机器人
        Role role = roleService.getRoleById(message.getCtId());
        if (ObjectUtils.isEmpty(role)) {
            session.setConversationStatus(Session.ConversationStatus.HUMAN);
        } else {
            session.setConversationStatus(Session.ConversationStatus.AI);
        }
        // 保存数据库
        save(session);
        // 存入Redis 是否有设置机器人
        stringRedisTemplate.opsForValue().set(KeyUtils.redisKeyUtils(key, session.getId()), JSONUtil.toJsonStr(session), timeout, TimeUnit.MINUTES);
        // session部分信息返回前端
        userServiceEndpoint.sendMessage(
                ChatMessage.builder()
                        .sessionId(session.getId())
                        .state(ChatMessage.State.SURE)
                        .build()
        );
        message.setSessionId(session.getId());
        return session;
    }
}