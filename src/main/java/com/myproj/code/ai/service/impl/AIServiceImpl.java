package com.myproj.code.ai.service.impl;

import cn.hutool.json.JSONUtil;
import com.myproj.code.ai.adviser.CustomizationMemoryAdviser;
import com.myproj.code.ai.service.AIService;
import com.myproj.code.entity.Role;
import com.myproj.code.entity.Session;
import com.myproj.code.mapper.RoleMapper;
import com.myproj.code.mapper.SessionMapper;
import com.myproj.code.utils.KeyUtils;
import com.myproj.code.websocket.endpoint.UserServiceEndpoint;
import com.myproj.code.websocket.message.ChatMessage;
import io.milvus.common.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {
    private final ChatClient chatClient;
    private final SessionMapper sessionMapper;
    private final RoleMapper roleMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final CustomizationMemoryAdviser customizationMemoryAdviser;

    @Value("${session.key}")
    private String sessionKey;
    @Value("${session.expiration-duration}")
    private Long timeout;
    @Value("classpath:template/convert-to-manual-judgment-prompts.st")
    private Resource aiPrompt;
    @Value("classpath:template/customer-service-role.st")
    private Resource aiPromptTemplate;
    @Override
    public Boolean turnToMunalJudgement(Session chatSession, ChatMessage chatMessage){
        boolean isTransfer = false;
        String result= chatClient.prompt()
                .system(aiPrompt)
                .user(chatMessage.getMessage())
                .call()
                .content();
        if(result !=null){
            isTransfer = Boolean.parseBoolean(result);
            if(isTransfer){
                // 更新数据库状态
                chatSession.setConversationStatus(Session.ConversationStatus.HUMAN);
                sessionMapper.updateById(chatSession);
                // 更新Redis
                stringRedisTemplate.opsForValue().set(KeyUtils.redisKeyUtils(sessionKey,chatSession.getId()), JSONUtil.toJsonStr(chatSession),timeout, TimeUnit.MINUTES);
            }

        }
        return isTransfer;
    }

    @Override
    public void chat (Session chatSession , ChatMessage chatMessage, UserServiceEndpoint userServiceEndpoint) throws IllegalAccessException {
        // 查询商户是否设置ai机器人
        Role role = roleMapper.selectById(chatSession.getCtId());
        // 构建提示词模板
        PromptTemplate promptTemplate = PromptTemplate.builder()
                .renderer(StTemplateRenderer.builder()
                        .startDelimiterToken('<')
                        .endDelimiterToken('>')
                        .build())
                .resource(aiPromptTemplate)
                .build();
        // 反射获取角色信息
        HashMap<String, Object> pos = new HashMap<>();
        Class<? extends Role> aClass = role.getClass();
        for (Field declaredField : aClass.getDeclaredFields()) {
            if(declaredField.getType()==String.class){
                declaredField.setAccessible(true);
                pos.put(declaredField.getName(),declaredField.get(role));
            }
        }
        // 流式输出
        Flux<ChatResponse> responseFlux = chatClient.prompt()
                .system(promptTemplate.render(pos))
                .user(chatMessage.getMessage())
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID,chatSession.getId()))
                .advisors(customizationMemoryAdviser)
                .stream().chatResponse();

    }
}
