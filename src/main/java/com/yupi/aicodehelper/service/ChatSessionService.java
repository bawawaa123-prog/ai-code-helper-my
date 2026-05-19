package com.yupi.aicodehelper.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.aicodehelper.model.dto.chat.ChatSessionCreateRequest;
import com.yupi.aicodehelper.model.entity.ChatSession;
import com.yupi.aicodehelper.model.vo.ChatSessionVO;
import java.util.List;

public interface ChatSessionService extends IService<ChatSession> {

    Long createSession(Long userId, ChatSessionCreateRequest request);

    List<ChatSessionVO> listMySessions(Long userId);

    void updateSessionTitle(Long userId, Long sessionId, String title);

    void deleteSession(Long userId, Long sessionId);

    void updateAfterChat(Long userId, Long sessionId, String lastMessage);

    void autoUpdateTitleIfNecessary(Long userId, Long sessionId, String userMessage);
}
