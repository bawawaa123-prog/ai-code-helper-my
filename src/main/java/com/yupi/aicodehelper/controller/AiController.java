package com.yupi.aicodehelper.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.aicodehelper.ai.AiCodeHelperServiceFactory;
import com.yupi.aicodehelper.ai.rag.RagQueryService;
import com.yupi.aicodehelper.ai.rag.RagProperties;
import com.yupi.aicodehelper.auth.LoginUserHolder;
import com.yupi.aicodehelper.common.ErrorCode;
import com.yupi.aicodehelper.exception.BusinessException;
import com.yupi.aicodehelper.model.dto.chat.ChatStreamRequest;
import com.yupi.aicodehelper.model.entity.ChatMessage;
import com.yupi.aicodehelper.model.entity.ChatSession;
import com.yupi.aicodehelper.model.entity.User;
import com.yupi.aicodehelper.service.ChatMessageService;
import com.yupi.aicodehelper.service.ChatSessionService;
import jakarta.annotation.Resource;
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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RestController
@RequestMapping("/ai")
public class AiController {

    private static final int HISTORY_MESSAGE_LIMIT = 10;
    private static final String SSE_EVENT_MESSAGE = "message";
    private static final String SSE_EVENT_SOURCES = "sources";
    private static final String SSE_EVENT_DONE = "done";

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
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话 id 不能为空");
        }
        if (!StringUtils.hasText(request.getMessage())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息不能为空");
        }
        ChatSession chatSession = chatSessionService.getById(request.getSessionId());
        if (chatSession == null || chatSession.getIsDelete() != null && chatSession.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "会话不存在");
        }
        if (!chatSession.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该会话");
        }

        boolean finalUseRag = request.getUseRag() != null
                ? request.getUseRag()
                : ragProperties.isEnabledByDefault();
        System.out.println("finalUseRag的值："+finalUseRag);
        String message = request.getMessage().trim();
        String memoryKey = loginUser.getId() + ":" + request.getSessionId();
        List<ChatMessage> recentMessages = chatMessageService.listRecentMessages(
                loginUser.getId(), request.getSessionId(), HISTORY_MESSAGE_LIMIT);
        System.out.println("最近对话的内容："+recentMessages);

        log.info("/ai/chat/stream request: userId={}, sessionId={}, useRag={}, memoryKey={}, recentMessageCount={}, messageLength={}",
                loginUser.getId(), request.getSessionId(), finalUseRag, memoryKey,
                recentMessages == null ? 0 : recentMessages.size(), message.length());
        log.debug("Recent chat messages for memory reload: {}", recentMessages);

        aiCodeHelperServiceFactory.reloadMemory(memoryKey, recentMessages);
        chatMessageService.saveUserMessage(loginUser.getId(), request.getSessionId(), message, finalUseRag);
        chatSessionService.autoUpdateTitleIfNecessary(loginUser.getId(), request.getSessionId(), message);
        StringBuilder assistantReplyBuilder = new StringBuilder();
        Long sessionId = request.getSessionId();
        Long userId = loginUser.getId();
        AtomicBoolean clientDisconnected = new AtomicBoolean(false);
        Flux<ServerSentEvent<String>> messageFlux = aiCodeHelperServiceFactory.chatStream(memoryKey, message, finalUseRag)
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
        return messageFlux.concatWith(Flux.defer(() -> buildTailEvents(finalUseRag, message, clientDisconnected.get())));
    }

    private Flux<ServerSentEvent<String>> buildTailEvents(boolean finalUseRag, String message, boolean clientDisconnected) {
        if (clientDisconnected) {
            return Flux.empty();
        }
        if (!finalUseRag) {
            log.debug("RAG disabled for this request, skip sources query. messageLength={}", message == null ? 0 : message.length());
            return Flux.just(buildEvent(SSE_EVENT_DONE, "done"));
        }
        log.debug("RAG enabled for this request, querying sources. messageLength={}", message == null ? 0 : message.length());
        String sourcesJson = serializeSources(message);
        log.debug("RAG sources query finished, sourcesJson={}", sourcesJson);
        return Flux.just(
                buildEvent(SSE_EVENT_SOURCES, sourcesJson),
                buildEvent(SSE_EVENT_DONE, "done")
        );
    }

    private String serializeSources(String message) {
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
                if (message != null && (message.contains("已建立的连接")
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
