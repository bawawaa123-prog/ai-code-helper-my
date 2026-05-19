package com.yupi.aicodehelper.model.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ChatMessageVO {

    private Long id;

    private Long sessionId;

    private Long userId;

    private String role;

    private String content;

    private Boolean ragEnabled;

    private String status;

    private LocalDateTime createTime;
}
