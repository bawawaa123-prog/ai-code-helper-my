package com.yupi.aicodehelper.model.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ChatSessionVO {

    private Long id;

    private Long userId;

    private String title;

    private String lastMessage;

    private Integer messageCount;

    private Boolean useRag;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
