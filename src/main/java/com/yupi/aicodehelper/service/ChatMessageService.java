package com.yupi.aicodehelper.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.aicodehelper.model.entity.ChatMessage;
import com.yupi.aicodehelper.model.vo.ChatMessageVO;
import java.util.List;

public interface ChatMessageService extends IService<ChatMessage> {

    List<ChatMessageVO> listSessionMessages(Long userId, Long sessionId);

    void saveUserMessage(Long userId, Long sessionId, String content, Boolean ragEnabled);

    void saveAssistantMessage(Long userId, Long sessionId, String content, Boolean ragEnabled, String status);
}
