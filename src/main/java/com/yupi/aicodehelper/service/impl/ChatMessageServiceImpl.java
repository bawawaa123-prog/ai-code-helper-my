package com.yupi.aicodehelper.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.aicodehelper.common.ErrorCode;
import com.yupi.aicodehelper.exception.BusinessException;
import com.yupi.aicodehelper.mapper.ChatMessageMapper;
import com.yupi.aicodehelper.model.entity.ChatMessage;
import com.yupi.aicodehelper.model.entity.ChatSession;
import com.yupi.aicodehelper.model.vo.ChatMessageVO;
import com.yupi.aicodehelper.service.ChatMessageService;
import com.yupi.aicodehelper.service.ChatSessionService;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatMessageService {

    @Resource
    private ChatSessionService chatSessionService;

    @Override
    public List<ChatMessageVO> listSessionMessages(Long userId, Long sessionId) {
        validateSessionAccess(userId, sessionId);

        return lambdaQuery()
                .eq(ChatMessage::getSessionId, sessionId)
                .eq(ChatMessage::getIsDelete, 0)
                .orderByAsc(ChatMessage::getCreateTime)
                .list()
                .stream()
                .map(this::toChatMessageVO)
                .toList();
    }

    @Override
    public List<ChatMessage> listRecentMessages(Long userId, Long sessionId, int limit) {
        validateSessionAccess(userId, sessionId);
        int finalLimit = limit > 0 ? limit : 10;

        List<ChatMessage> recentMessages = lambdaQuery()
                .eq(ChatMessage::getUserId, userId)
                .eq(ChatMessage::getSessionId, sessionId)
                .eq(ChatMessage::getIsDelete, 0)
                .eq(ChatMessage::getStatus, "success")
                .in(ChatMessage::getRole, "user", "assistant")
                .orderByDesc(ChatMessage::getCreateTime)
                .last("limit " + finalLimit)
                .list();

        List<ChatMessage> orderedMessages = new ArrayList<>(recentMessages);
        Collections.reverse(orderedMessages);
        return orderedMessages;
    }

    @Override
    public void saveUserMessage(Long userId, Long sessionId, String content, Boolean ragEnabled) {
        saveMessage(userId, sessionId, "user", content, ragEnabled, "success");
    }

    @Override
    public void saveAssistantMessage(Long userId, Long sessionId, String content, Boolean ragEnabled, String status) {
        saveMessage(userId, sessionId, "assistant", content, ragEnabled, status);
    }

    private void saveMessage(Long userId, Long sessionId, String role, String content, Boolean ragEnabled, String status) {
        if (userId == null || sessionId == null || !StringUtils.hasText(content)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息保存参数不完整");
        }
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setUserId(userId);
        chatMessage.setSessionId(sessionId);
        chatMessage.setRole(role);
        chatMessage.setContent(content);
        chatMessage.setRagEnabled(Boolean.TRUE.equals(ragEnabled) ? 1 : 0);
        chatMessage.setStatus(status);
        chatMessage.setIsDelete(0);
        boolean saved = save(chatMessage);
        if (!saved) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存聊天消息失败");
        }
    }

    private void validateSessionAccess(Long userId, Long sessionId) {
        if (sessionId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "会话 id 不能为空");
        }
        ChatSession chatSession = chatSessionService.getById(sessionId);
        if (chatSession == null || chatSession.getIsDelete() != null && chatSession.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "会话不存在");
        }
        if (!chatSession.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该会话");
        }
    }

    private ChatMessageVO toChatMessageVO(ChatMessage chatMessage) {
        ChatMessageVO chatMessageVO = new ChatMessageVO();
        chatMessageVO.setId(chatMessage.getId());
        chatMessageVO.setSessionId(chatMessage.getSessionId());
        chatMessageVO.setUserId(chatMessage.getUserId());
        chatMessageVO.setRole(chatMessage.getRole());
        chatMessageVO.setContent(chatMessage.getContent());
        chatMessageVO.setRagEnabled(chatMessage.getRagEnabled() != null && chatMessage.getRagEnabled() == 1);
        chatMessageVO.setStatus(chatMessage.getStatus());
        chatMessageVO.setCreateTime(chatMessage.getCreateTime());
        return chatMessageVO;
    }
}
