package com.yupi.aicodehelper.model.dto.chat;

import lombok.Data;

@Data
public class ChatStreamRequest {

    private Long sessionId;

    private String message;

    private Boolean useRag;
}
