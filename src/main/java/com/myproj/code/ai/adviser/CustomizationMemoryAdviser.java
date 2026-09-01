package com.myproj.code.ai.adviser;

import com.myproj.code.ai.memory.CustomizationMemory;
import jakarta.annotation.Resource;
import opennlp.tools.util.featuregen.AggregatedFeatureGenerator;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CustomizationMemoryAdviser implements BaseChatMemoryAdvisor {
    @Resource
    private final PromptTemplate systemPromptTemplate = new PromptTemplate("{instructions}\n\nUse the conversation memory from the MEMORY section to provide accurate answers.\n\n---------------------\nMEMORY:\n{memory}\n---------------------\n\n");

    @Resource
    private CustomizationMemory customizationMemory;

    /**
     * 发送前的操作：
     * <p>
     * 此方法在发送消息前执行，主要功能包括：
     * </p>
     *
     * <ol>
     *     <li>从聊天中提取发送的消息</li>
     *
     *     <li>根据会话ID查询记忆</li>
     *
     *     <li>首先从Redis中查询最近的几条记忆</li>
     *
     *     <li>如果Redis中没有，则从MySQL中查询最近的10条记忆</li>
     *
     *     <li>将当前记忆异步添加到Redis和MySQL中</li>
     *
     *     <li>将查询到的记忆整合到新的prompt中</li>
     *
     *     <li>返回增强后的prompt给chatClientRequest</li>
     *
     * </ol>
     *
     * @param chatClientRequest 待处理的ChatClientRequest对象
     * @param advisorChain      AdvisorChain对象，用于获取其他Advisor
     * @return ChatClientRequest对象，包含增强后的prompt
     */
    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        Map<String, Object> context = chatClientRequest.context();
        // 上下文获取会话id
        String conversationId = getConversationId(context, "default");
        if ("default".equals(conversationId)) {
            throw new RuntimeException("会话id不能为空");
        }
        // 根据会话id获取上下文记忆
        List<Message> messageList = customizationMemory.get(conversationId);
        // 写入提示词 记忆
        String memory = messageList.stream().filter((m) -> m.getMessageType() == MessageType.USER || m.getMessageType() == MessageType.ASSISTANT).map((m) -> {
            String identify = String.valueOf(m.getMessageType());
            return identify + ":" + m.getText();
        }).collect(Collectors.joining(System.lineSeparator()));
        // 获取系统提示词
        SystemMessage systemMessage = chatClientRequest.prompt().getSystemMessage();
        // 整合提示词
        String augmentedSystemText = systemPromptTemplate.render(Map.of("instructions", systemMessage.getText(), "memory", memory));
        // 复制一个聊天，并加上新提示词
        ChatClientRequest build = chatClientRequest.mutate()
                .prompt(chatClientRequest.prompt()
                        .augmentSystemMessage(augmentedSystemText))
                .build();
        // 提取用户信息，并存储
        UserMessage userMessage = chatClientRequest.prompt().getUserMessage();
        customizationMemory.add(conversationId, userMessage);
        return build;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        // AI返回的内容进行存储
        String conversationId = getConversationId(chatClientResponse.context(), "default");
        if ("default".equals(conversationId)) {
            throw new RuntimeException("会话id不能为空");
        }
        // 获取AI信息
        ChatResponse chatResponse = chatClientResponse.chatResponse();
        if (chatResponse == null) {
            throw new RuntimeException("ai消息回复为空");
        }
        StringBuffer content = new StringBuffer();
        for (Generation result : chatResponse.getResults()) {
            content.append(result.getOutput().getText());
        }
        AssistantMessage assistantMessage = new AssistantMessage(content.toString());
        // 存储
        this.customizationMemory.add(conversationId, assistantMessage);
        return chatClientResponse;
    }

    /**
     * 整合执行流程
     *
     * @param chatClientRequest
     * @param streamAdvisorChain
     * @return
     */
    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        // 先执行befor
        ChatClientRequest befored = before(chatClientRequest, streamAdvisorChain);
        // 整合
        Flux<ChatClientResponse> responseFlux = streamAdvisorChain.nextStream(befored);

        return new ChatClientMessageAggregator().aggregateChatClientResponse(
                responseFlux,
                aggregated -> {
                    after(aggregated, streamAdvisorChain);
                }
        );
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
