package com.yupi.aicodehelper.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.aicodehelper.ai.AiCodeHelperServiceFactory;
import com.yupi.aicodehelper.ai.rag.RagProperties;
import com.yupi.aicodehelper.ai.rag.RagQueryService;
import com.yupi.aicodehelper.auth.LoginUserHolder;
import com.yupi.aicodehelper.common.ErrorCode;
import com.yupi.aicodehelper.exception.BusinessException;
import com.yupi.aicodehelper.model.dto.chat.ChatStreamRequest;
import com.yupi.aicodehelper.model.dto.knowledge.KnowledgeSearchRequest;
import com.yupi.aicodehelper.model.entity.ChatMessage;
import com.yupi.aicodehelper.model.entity.ChatSession;
import com.yupi.aicodehelper.model.entity.User;
import com.yupi.aicodehelper.model.vo.RagSourceVO;
import com.yupi.aicodehelper.service.ChatMessageService;
import com.yupi.aicodehelper.service.ChatSessionService;
import com.yupi.aicodehelper.service.KnowledgeSearchService;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/ai")
public class AiController {

    private static final int HISTORY_MESSAGE_LIMIT = 10;
    private static final int KNOWLEDGE_SEARCH_MAX_RESULTS = 5;
    private static final double KNOWLEDGE_SEARCH_MIN_SCORE = 0.6D;
    private static final String SSE_EVENT_MESSAGE = "message";
    private static final String SSE_EVENT_SOURCES = "sources";
    private static final String SSE_EVENT_DONE = "done";
    private static final String KNOWLEDGE_MISSING_NOTICE = "\u77e5\u8bc6\u5e93\u4e2d\u6ca1\u6709\u8db3\u591f\u4f9d\u636e";

    @Resource
    private AiCodeHelperServiceFactory aiCodeHelperServiceFactory;

    @Resource
    private RagQueryService ragQueryService;

    @Resource
    private RagProperties ragProperties;

    @Resource
    private ChatSessionService chatSessionService;

    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private KnowledgeSearchService knowledgeSearchService;

    @Resource
    private ObjectMapper objectMapper;

    @GetMapping("/chat")
    public Flux<ServerSentEvent<String>> chat(int memoryId, String message,
                                              @RequestParam(required = false) Boolean useRag) {
        boolean finalUseRag = useRag != null ? useRag : ragProperties.isEnabledByDefault();
        log.info("/ai/chat request: memoryId={}, useRag={}, messageLength={}", memoryId, finalUseRag,
                message == null ? 0 : message.length());
        return aiCodeHelperServiceFactory.chatStream(String.valueOf(memoryId), message, finalUseRag)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(@RequestBody ChatStreamRequest request) {
        User loginUser = LoginUserHolder.get();
        if (request == null || request.getSessionId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "\u4f1a\u8bdd id \u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (!StringUtils.hasText(request.getMessage())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "\u6d88\u606f\u4e0d\u80fd\u4e3a\u7a7a");
        }

        ChatSession chatSession = chatSessionService.getById(request.getSessionId());
        if (chatSession == null || Integer.valueOf(1).equals(chatSession.getIsDelete())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "\u4f1a\u8bdd\u4e0d\u5b58\u5728");
        }
        if (!chatSession.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "\u65e0\u6743\u9650\u8bbf\u95ee\u8be5\u4f1a\u8bdd");
        }

        boolean finalUseRag = request.getUseRag() != null
                ? request.getUseRag()
                : ragProperties.isEnabledByDefault();
        Long knowledgeBaseId = request.getKnowledgeBaseId();
        boolean useKnowledgeBaseRag = finalUseRag && knowledgeBaseId != null;
        String originalMessage = request.getMessage().trim();
        String memoryKey = loginUser.getId() + ":" + request.getSessionId();
        List<ChatMessage> recentMessages = chatMessageService.listRecentMessages(
                loginUser.getId(), request.getSessionId(), HISTORY_MESSAGE_LIMIT);
        List<RagSourceVO> knowledgeSources = useKnowledgeBaseRag
                ? searchKnowledgeSources(loginUser.getId(), knowledgeBaseId, originalMessage)
                : Collections.emptyList();
        String aiMessage = useKnowledgeBaseRag
                ? buildKnowledgeEnhancedMessage(originalMessage, knowledgeSources)
                : originalMessage;
        boolean aiUseRag = finalUseRag && !useKnowledgeBaseRag;

        log.info("/ai/chat/stream request: userId={}, sessionId={}, useRag={}, knowledgeBaseId={}, useKnowledgeBaseRag={}, memoryKey={}, recentMessageCount={}, messageLength={}",
                loginUser.getId(), request.getSessionId(), finalUseRag, knowledgeBaseId, useKnowledgeBaseRag, memoryKey,
                recentMessages == null ? 0 : recentMessages.size(), originalMessage.length());
        log.debug("Recent chat messages for memory reload: {}", recentMessages);

        aiCodeHelperServiceFactory.reloadMemory(memoryKey, recentMessages);
        chatMessageService.saveUserMessage(loginUser.getId(), request.getSessionId(), originalMessage, finalUseRag);
        chatSessionService.autoUpdateTitleIfNecessary(loginUser.getId(), request.getSessionId(), originalMessage);

        StringBuilder assistantReplyBuilder = new StringBuilder();
        Long sessionId = request.getSessionId();
        Long userId = loginUser.getId();
        AtomicBoolean clientDisconnected = new AtomicBoolean(false);
        Flux<ServerSentEvent<String>> messageFlux = aiCodeHelperServiceFactory.chatStream(memoryKey, aiMessage, aiUseRag)
                .doOnNext(assistantReplyBuilder::append)
                .map(chunk -> buildEvent(SSE_EVENT_MESSAGE, chunk))
                .doOnComplete(() -> {
                    String assistantReply = assistantReplyBuilder.toString();
                    chatMessageService.saveAssistantMessage(userId, sessionId, assistantReply, finalUseRag, "success");
                    chatSessionService.updateAfterChat(userId, sessionId, assistantReply);
                })
                .onErrorResume(this::isClientDisconnectError, error -> {
                    clientDisconnected.set(true);
                    log.info("SSE chat stream closed by client: sessionId={}, userId={}", sessionId, userId);
                    return Flux.empty();
                })
                .doOnError(error -> {
                    String assistantReply = assistantReplyBuilder.toString();
                    if (StringUtils.hasText(assistantReply)) {
                        chatMessageService.saveAssistantMessage(userId, sessionId, assistantReply, finalUseRag, "error");
                        chatSessionService.updateAfterChat(userId, sessionId, assistantReply);
                    }
                });
        return messageFlux.concatWith(Flux.defer(() -> buildTailEvents(
                finalUseRag, knowledgeBaseId, originalMessage, knowledgeSources, clientDisconnected.get())));
    }

    private Flux<ServerSentEvent<String>> buildTailEvents(boolean finalUseRag,
                                                          Long knowledgeBaseId,
                                                          String originalMessage,
                                                          List<RagSourceVO> knowledgeSources,
                                                          boolean clientDisconnected) {
        if (clientDisconnected) {
            return Flux.empty();
        }
        if (!finalUseRag) {
            log.debug("RAG disabled for this request, skip sources query. messageLength={}",
                    originalMessage == null ? 0 : originalMessage.length());
            return Flux.just(buildEvent(SSE_EVENT_DONE, "done"));
        }
        String sourcesJson = knowledgeBaseId != null
                ? serializeKnowledgeSources(knowledgeSources)
                : serializeStaticRagSources(originalMessage);
        return Flux.just(
                buildEvent(SSE_EVENT_SOURCES, sourcesJson),
                buildEvent(SSE_EVENT_DONE, "done")
        );
    }

    private List<RagSourceVO> searchKnowledgeSources(Long userId, Long knowledgeBaseId, String originalMessage) {
        KnowledgeSearchRequest searchRequest = new KnowledgeSearchRequest();
        searchRequest.setQuery(originalMessage);
        searchRequest.setMaxResults(KNOWLEDGE_SEARCH_MAX_RESULTS);
        searchRequest.setMinScore(KNOWLEDGE_SEARCH_MIN_SCORE);
        return knowledgeSearchService.searchKnowledgeBase(userId, knowledgeBaseId, searchRequest);
    }

    private String buildKnowledgeEnhancedMessage(String originalMessage, List<RagSourceVO> knowledgeSources) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("You are an AI programming coach. Please answer with priority based on the user's knowledge base references below.\n")
                .append("If the references are insufficient, explicitly say \"")
                .append(KNOWLEDGE_MISSING_NOTICE)
                .append("\" and then provide general advice.\n\n")
                .append("[User Knowledge Base References]\n");
        if (knowledgeSources == null || knowledgeSources.isEmpty()) {
            promptBuilder.append("No relevant content was retrieved from the knowledge base.\n\n");
        } else {
            for (int i = 0; i < knowledgeSources.size(); i++) {
                RagSourceVO source = knowledgeSources.get(i);
                String sourceName = StringUtils.hasText(source.getSourceName()) ? source.getSourceName() : "Unknown source";
                String content = StringUtils.hasText(source.getContent()) ? source.getContent() : "No content";
                promptBuilder.append("[")
                        .append(i + 1)
                        .append("] Source: ")
                        .append(sourceName)
                        .append("\nContent: ")
                        .append(content)
                        .append("\n\n");
            }
        }
        promptBuilder.append("[User Question]\n")
                .append(originalMessage);
        return promptBuilder.toString();
    }

    private String serializeStaticRagSources(String message) {
        try {
            return objectMapper.writeValueAsString(ragQueryService.querySources(message));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize RAG sources, return empty list. message={}", message, e);
            return "[]";
        } catch (Exception e) {
            log.warn("Failed to query RAG sources, return empty list. message={}", message, e);
            try {
                return objectMapper.writeValueAsString(Collections.emptyList());
            } catch (JsonProcessingException jsonProcessingException) {
                return "[]";
            }
        }
    }

    private String serializeKnowledgeSources(List<RagSourceVO> knowledgeSources) {
        try {
            return objectMapper.writeValueAsString(
                    knowledgeSources == null ? Collections.emptyList() : knowledgeSources);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize knowledge base sources, return empty list.", e);
            return "[]";
        }
    }

    private ServerSentEvent<String> buildEvent(String event, String data) {
        return ServerSentEvent.<String>builder()
                .event(event)
                .data(data)
                .build();
    }

    private boolean isClientDisconnectError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof CancellationException) {
                return true;
            }
            if (current instanceof IOException ioException) {
                String message = ioException.getMessage();
                if (message != null && (message.contains("\u5df2\u5efa\u7acb\u7684\u8fde\u63a5")
                        || message.contains("An established connection")
                        || message.contains("Broken pipe")
                        || message.contains("Connection reset"))) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
