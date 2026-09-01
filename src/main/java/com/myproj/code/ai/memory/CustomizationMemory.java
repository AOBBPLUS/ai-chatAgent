package com.myproj.code.ai.memory;

import cn.hutool.json.JSONUtil;
import com.myproj.code.entity.Session;
import com.myproj.code.entity.SessionLog;
import com.myproj.code.executor.GlobalTreadPool;
import com.myproj.code.service.SessionLogService;
import com.myproj.code.service.SessionService;
import com.myproj.code.utils.KeyUtils;
import com.myproj.code.utils.TypeConversion;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Component
public class CustomizationMemory implements ChatMemory {
    @Value("${memory.key}")
    private String memoryKey;
    @Value("${memory.expiration-duration}")
    private Integer timeout;
    @Value("${memory.redis-length}")
    private Integer memorySize;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private SessionLogService sessionLogService;

    @Override
    public void add(String conversationId, List<Message> messages) {
        GlobalTreadPool.executor.execute(() -> sessionLogService.add(conversationId, messages));
    }

    @Override
    public List<Message> get(String conversationId) {
        List<String> range = stringRedisTemplate.opsForList().range(KeyUtils.redisKeyUtils(memoryKey, conversationId), -memorySize, -1);
        if (range == null || range.isEmpty()) {
            //Redis没有数据
            // 查询数据库
            List<SessionLog> sessionLogs = sessionLogService.tryGetSessionLogs(conversationId);
            // 插入Redis[单开线程]
            GlobalTreadPool.executor.execute(() -> sessionLogService.addToRedis(conversationId, sessionLogs));
            // session->message
            List<Message> messages = new ArrayList<>(sessionLogs.size());
            for (SessionLog sessionLog : sessionLogs) {
                messages.add(TypeConversion.sessionToMessage(sessionLog.getType(), sessionLog.getContent()));
            }
            return messages;

        }
        List<Message> messages = new ArrayList<>(range.size());
        for (String jsonStr : range) {
            SessionLog bean = JSONUtil.toBean(jsonStr, SessionLog.class);
            messages.add(TypeConversion.sessionToMessage(bean.getType(),bean.getContent()));
        }
        return messages;
    }

    @Override
    public void clear(String conversationId) {
//        stringRedisTemplate.opsForList().remove(KeyUtils.redisKeyUtils(memoryKey, conversationId),)
    }
}
