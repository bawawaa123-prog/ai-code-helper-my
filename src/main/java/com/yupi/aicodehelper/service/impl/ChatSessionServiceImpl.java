package com.yupi.aicodehelper.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.aicodehelper.common.ErrorCode;
import com.yupi.aicodehelper.exception.BusinessException;
import com.yupi.aicodehelper.mapper.ChatSessionMapper;
import com.yupi.aicodehelper.model.dto.chat.ChatSessionCreateRequest;
import com.yupi.aicodehelper.model.entity.ChatSession;
import com.yupi.aicodehelper.model.vo.ChatSessionVO;
import com.yupi.aicodehelper.service.ChatSessionService;
import jakarta.annotation.Resource;
import java.time.Duration;
import java.util.List;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements ChatSessionService {

    private static final String DEFAULT_TITLE = "新会话";

    private static final int MAX_TITLE_LENGTH = 128;

    private static final int AUTO_TITLE_DISPLAY_LENGTH = 20;

    private static final int LAST_MESSAGE_MAX_LENGTH = 512;

    private static final String SESSION_LIST_CACHE_KEY_PREFIX = "chat:sessions:v2:";

    private static final Duration SESSION_LIST_CACHE_TTL = Duration.ofMinutes(30);

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Long createSession(Long userId, ChatSessionCreateRequest request) {
        ChatSession chatSession = new ChatSession();
        chatSession.setUserId(userId);
        chatSession.setTitle(resolveTitle(request));
        chatSession.setUseRag(resolveUseRag(request) ? 1 : 0);
        chatSession.setMessageCount(0);
        chatSession.setStatus(1);
        boolean saved = save(chatSession);
        if (!saved || chatSession.getId() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建会话失败");
        }
        deleteSessionListCache(userId);
        return chatSession.getId();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ChatSessionVO> listMySessions(Long userId) {
        String cacheKey = buildSessionListCacheKey(userId);
        Object cachedValue = getCachedValueSafely(cacheKey);
        if (cachedValue instanceof List<?>) {
            return (List<ChatSessionVO>) cachedValue;
        }

        List<ChatSessionVO> sessionList = lambdaQuery()
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getIsDelete, 0)
                .orderByDesc(ChatSession::getCreateTime)
                .list()
                .stream()
                .map(this::toChatSessionVO)
                .toList();
        redisTemplate.opsForValue().set(cacheKey, sessionList, SESSION_LIST_CACHE_TTL);
        return sessionList;
    }

    @Override
    public void updateSessionTitle(Long userId, Long sessionId, String title) {
        if (sessionId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话 id 不能为空");
        }
        if (!StringUtils.hasText(title)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话标题不能为空");
        }
        String trimmedTitle = title.trim();
        if (trimmedTitle.length() > MAX_TITLE_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话标题长度不能超过 128");
        }

        ChatSession chatSession = getById(sessionId);
        validateOwnedSession(userId, chatSession);

        boolean updated = lambdaUpdate()
                .eq(ChatSession::getId, sessionId)
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getIsDelete, 0)
                .set(ChatSession::getTitle, trimmedTitle)
                .update();
        if (!updated) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新会话标题失败");
        }
        deleteSessionListCache(userId);
    }

    @Override
    public void deleteSession(Long userId, Long sessionId) {
        if (sessionId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话 id 不能为空");
        }

        ChatSession chatSession = getById(sessionId);
        validateOwnedSession(userId, chatSession);

        boolean updated = lambdaUpdate()
                .eq(ChatSession::getId, sessionId)
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getIsDelete, 0)
                .set(ChatSession::getIsDelete, 1)
                .update();
        if (!updated) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除会话失败");
        }
        deleteSessionListCache(userId);
    }

    @Override
    public void updateAfterChat(Long userId, Long sessionId, String lastMessage) {
        if (sessionId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话 id 不能为空");
        }

        ChatSession chatSession = getById(sessionId);
        validateOwnedSession(userId, chatSession);

        int currentMessageCount = chatSession.getMessageCount() == null ? 0 : chatSession.getMessageCount();
        boolean updated = lambdaUpdate()
                .eq(ChatSession::getId, sessionId)
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getIsDelete, 0)
                .set(ChatSession::getLastMessage, truncateLastMessage(lastMessage))
                .set(ChatSession::getMessageCount, currentMessageCount + 2)
                .update();
        if (!updated) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新会话信息失败");
        }
        deleteSessionListCache(userId);
    }

    @Override
    public void autoUpdateTitleIfNecessary(Long userId, Long sessionId, String userMessage) {
        ChatSession chatSession = getById(sessionId);
        validateOwnedSession(userId, chatSession);
        if (!shouldAutoUpdateTitle(chatSession.getTitle())) {
            return;
        }

        String generatedTitle = generateAutoTitle(userMessage);
        if (!StringUtils.hasText(generatedTitle)) {
            return;
        }

        boolean updated = lambdaUpdate()
                .eq(ChatSession::getId, sessionId)
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getIsDelete, 0)
                .set(ChatSession::getTitle, generatedTitle)
                .update();
        if (!updated) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "自动更新会话标题失败");
        }
        deleteSessionListCache(userId);
    }

    private void validateOwnedSession(Long userId, ChatSession chatSession) {
        if (chatSession == null || chatSession.getIsDelete() != null && chatSession.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "会话不存在");
        }
        if (!chatSession.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该会话");
        }
    }

    private boolean shouldAutoUpdateTitle(String title) {
        return !StringUtils.hasText(title) || DEFAULT_TITLE.equals(title.trim());
    }

    private String generateAutoTitle(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return null;
        }
        String normalized = userMessage.trim()
                .replace("\r", " ")
                .replace("\n", " ");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (normalized.length() > AUTO_TITLE_DISPLAY_LENGTH) {
            normalized = normalized.substring(0, AUTO_TITLE_DISPLAY_LENGTH) + "...";
        }
        if (normalized.length() > MAX_TITLE_LENGTH) {
            normalized = normalized.substring(0, MAX_TITLE_LENGTH);
        }
        return normalized;
    }

    private String truncateLastMessage(String lastMessage) {
        if (lastMessage == null) {
            return null;
        }
        String trimmed = lastMessage.trim();
        if (trimmed.length() <= LAST_MESSAGE_MAX_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, LAST_MESSAGE_MAX_LENGTH);
    }

    private String resolveTitle(ChatSessionCreateRequest request) {
        if (request == null || !StringUtils.hasText(request.getTitle())) {
            return DEFAULT_TITLE;
        }
        return request.getTitle().trim();
    }

    private boolean resolveUseRag(ChatSessionCreateRequest request) {
        return request != null && Boolean.TRUE.equals(request.getUseRag());
    }

    private String buildSessionListCacheKey(Long userId) {
        return SESSION_LIST_CACHE_KEY_PREFIX + userId;
    }

    private void deleteSessionListCache(Long userId) {
        redisTemplate.delete(buildSessionListCacheKey(userId));
    }

    private Object getCachedValueSafely(String cacheKey) {
        try {
            return redisTemplate.opsForValue().get(cacheKey);
        } catch (SerializationException e) {
            redisTemplate.delete(cacheKey);
            return null;
        }
    }

    private ChatSessionVO toChatSessionVO(ChatSession chatSession) {
        ChatSessionVO chatSessionVO = new ChatSessionVO();
        chatSessionVO.setId(chatSession.getId());
        chatSessionVO.setUserId(chatSession.getUserId());
        chatSessionVO.setTitle(chatSession.getTitle());
        chatSessionVO.setLastMessage(chatSession.getLastMessage());
        chatSessionVO.setMessageCount(chatSession.getMessageCount());
        chatSessionVO.setUseRag(chatSession.getUseRag() != null && chatSession.getUseRag() == 1);
        chatSessionVO.setCreateTime(chatSession.getCreateTime());
        chatSessionVO.setUpdateTime(chatSession.getUpdateTime());
        return chatSessionVO;
    }
}
