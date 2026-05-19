package com.yupi.aicodehelper.model.dto.chat;

import lombok.Data;

@Data
public class ChatSessionCreateRequest {

    private String title;

    private Boolean useRag;
}
