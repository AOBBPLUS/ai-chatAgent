package com.myproj.code.utils;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.myproj.code.entity.Session;
import com.myproj.code.mapper.SessionMapper;
import com.myproj.code.service.SessionService;
import com.myproj.code.websocket.endpoint.CommercialTenantEndpoint;
import com.myproj.code.websocket.endpoint.UserServiceEndpoint;
import jakarta.annotation.Resource;
import jakarta.websocket.EncodeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class SessionFind {

    @Value("${session.key}")
    private String key;
    @Value("${session.expiration-duration}")
    private Integer timeout;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private SessionMapper sessionMapper;

    public Session getSessionById(Integer sessionId) throws EncodeException, IOException {

        // 查找Redis
        String json = stringRedisTemplate.opsForValue().getAndExpire(KeyUtils.redisKeyUtils(key, sessionId.toString()), timeout, TimeUnit.MINUTES);
        Session session = null;
        if (json == null) { // 不存在就通过数据库查询
            session = sessionMapper.selectById(sessionId);
            if (session == null) {
                throw new RuntimeException("未查询到对应对应对话");
            }
            // 存入Redis
            stringRedisTemplate.opsForValue().set(KeyUtils.redisKeyUtils(key, sessionId.toString()), JSONUtil.toJsonStr(session), timeout, TimeUnit.MINUTES);
        } else {
            session = JSONUtil.toBean(json, Session.class);
        }
        return session;
    }

    public UserServiceEndpoint getUserEndPointById(Integer sessionId) throws EncodeException, IOException {
        Session session = getSessionById(sessionId); //这里要先找Session
        // 通过session中的userId查找对应的用户的endPoint
        return UserServiceEndpoint.findEndPoint(session.getUserId());
    }

    public CommercialTenantEndpoint getCTEndPointById(Integer sessionId) throws EncodeException, IOException {
        Session sessionById = getSessionById(sessionId);
        return CommercialTenantEndpoint.findEndPoint(sessionById.getId());
    }
}
