package com.yupi.aicodehelper.controller;

import com.yupi.aicodehelper.auth.LoginUserHolder;
import com.yupi.aicodehelper.common.BaseResponse;
import com.yupi.aicodehelper.common.ResultUtils;
import com.yupi.aicodehelper.model.dto.chat.ChatSessionCreateRequest;
import com.yupi.aicodehelper.model.dto.chat.ChatSessionUpdateRequest;
import com.yupi.aicodehelper.model.entity.User;
import com.yupi.aicodehelper.model.vo.ChatMessageVO;
import com.yupi.aicodehelper.model.vo.ChatSessionVO;
import com.yupi.aicodehelper.service.ChatMessageService;
import com.yupi.aicodehelper.service.ChatSessionService;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat/session")
public class ChatSessionController {

    @Resource
    private ChatSessionService chatSessionService;

    @Resource
    private ChatMessageService chatMessageService;

    @PostMapping
    public BaseResponse<Long> createSession(@RequestBody(required = false) ChatSessionCreateRequest request) {
        User loginUser = LoginUserHolder.get();
        Long sessionId = chatSessionService.createSession(loginUser.getId(), request);
        return ResultUtils.success(sessionId);
    }

    @GetMapping("/list")
    public BaseResponse<List<ChatSessionVO>> listMySessions() {
        User loginUser = LoginUserHolder.get();
        List<ChatSessionVO> sessionList = chatSessionService.listMySessions(loginUser.getId());
        return ResultUtils.success(sessionList);
    }

    @PutMapping("/{sessionId}")
    public BaseResponse<Boolean> updateSessionTitle(@PathVariable Long sessionId,
                                                    @RequestBody ChatSessionUpdateRequest request) {
        User loginUser = LoginUserHolder.get();
        chatSessionService.updateSessionTitle(loginUser.getId(), sessionId, request == null ? null : request.getTitle());
        return ResultUtils.success(Boolean.TRUE);
    }

    @DeleteMapping("/{sessionId}")
    public BaseResponse<Boolean> deleteSession(@PathVariable Long sessionId) {
        User loginUser = LoginUserHolder.get();
        chatSessionService.deleteSession(loginUser.getId(), sessionId);
        return ResultUtils.success(Boolean.TRUE);
    }

    @GetMapping("/{sessionId}/messages")
    public BaseResponse<List<ChatMessageVO>> listSessionMessages(@PathVariable Long sessionId) {
        User loginUser = LoginUserHolder.get();
        List<ChatMessageVO> messageList = chatMessageService.listSessionMessages(loginUser.getId(), sessionId);
        return ResultUtils.success(messageList);
    }
}
