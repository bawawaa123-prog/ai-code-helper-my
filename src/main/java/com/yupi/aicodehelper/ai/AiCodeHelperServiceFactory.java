package com.yupi.aicodehelper.ai;


import com.yupi.aicodehelper.ai.tools.InterviewQuestionTool;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

@Component
public class AiCodeHelperServiceFactory {

    private static final int MAX_MEMORY_MESSAGES = 10;

    @Resource
    private ChatModel myQwenChatModel;

    @Resource
    private ObjectProvider<ContentRetriever> contentRetrieverProvider;

    @Resource
    private StreamingChatModel qwenStreamingChatModel;

    private volatile AiCodeHelperService defaultAiCodeHelperService;

    private volatile AiCodeHelperService ragAiCodeHelperService;

    private final Map<String, ChatMemory> chatMemoryMap = new ConcurrentHashMap<>();

    public Flux<String> chatStream(String memoryId, String message, boolean useRag) {
        return getAiCodeHelperService(useRag).chatStream(memoryId, message);
    }

    public ChatMemory getOrCreateMemory(String memoryKey) {
        return chatMemoryMap.computeIfAbsent(memoryKey,
                key -> MessageWindowChatMemory.builder()
                        .id(key)
                        .maxMessages(MAX_MEMORY_MESSAGES)
                        .build());
    }

    public void reloadMemory(String memoryKey, List<com.yupi.aicodehelper.model.entity.ChatMessage> messages) {
        ChatMemory chatMemory = getOrCreateMemory(memoryKey);
        chatMemory.clear();
        if (messages == null || messages.isEmpty()) {
            return;
        }
        messages.stream()
                .map(this::toLangChainMessage)
                .filter(java.util.Objects::nonNull)
                .forEach(chatMemory::add);
    }

    public AiCodeHelperService getAiCodeHelperService(boolean useRag) {
        if (useRag) {
            if (ragAiCodeHelperService == null) {
                synchronized (this) {
                    if (ragAiCodeHelperService == null) {
                        ragAiCodeHelperService = buildAiCodeHelperService(contentRetrieverProvider.getObject());
                    }
                }
            }
            return ragAiCodeHelperService;
        }
        if (defaultAiCodeHelperService == null) {
            synchronized (this) {
                if (defaultAiCodeHelperService == null) {
                    defaultAiCodeHelperService = buildAiCodeHelperService(null);
                }
            }
        }
        return defaultAiCodeHelperService;
    }

    private AiCodeHelperService buildAiCodeHelperService(ContentRetriever contentRetriever) {
        var builder = AiServices.builder(AiCodeHelperService.class)
                .chatModel(myQwenChatModel)
                .streamingChatModel(qwenStreamingChatModel)
                .chatMemoryProvider(memoryId -> getOrCreateMemory(String.valueOf(memoryId)))
                .tools(new InterviewQuestionTool());
        if (contentRetriever != null) {
            builder.contentRetriever(contentRetriever);
        }
        return builder.build();
    }

    private ChatMessage toLangChainMessage(com.yupi.aicodehelper.model.entity.ChatMessage chatMessage) {
        if (chatMessage == null || !StringUtils.hasText(chatMessage.getContent())) {
            return null;
        }
        String role = chatMessage.getRole();
        if ("user".equals(role)) {
            return UserMessage.from(chatMessage.getContent());
        }
        if ("assistant".equals(role)) {
            return AiMessage.from(chatMessage.getContent());
        }
        return null;
    }
}
