package com.yupi.aicodehelper.ai;


import com.yupi.aicodehelper.ai.tools.InterviewQuestionTool;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class AiCodeHelperServiceFactory {

    @Resource
    private ChatModel myQwenChatModel;

    @Resource
    private ObjectProvider<ContentRetriever> contentRetrieverProvider;

    @Resource
    private StreamingChatModel qwenStreamingChatModel;

    private volatile AiCodeHelperService defaultAiCodeHelperService;

    private volatile AiCodeHelperService ragAiCodeHelperService;

    public Flux<String> chatStream(String memoryId, String message, boolean useRag) {
        return getAiCodeHelperService(useRag).chatStream(memoryId, message);
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
        // 会话记忆
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);
        var builder = AiServices.builder(AiCodeHelperService.class)
                .chatModel(myQwenChatModel)
                .streamingChatModel(qwenStreamingChatModel)
                .chatMemory(chatMemory)
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.withMaxMessages(10))
                .tools(new InterviewQuestionTool());
        if (contentRetriever != null) {
            builder.contentRetriever(contentRetriever);
        }
        return builder.build();
    }
}
