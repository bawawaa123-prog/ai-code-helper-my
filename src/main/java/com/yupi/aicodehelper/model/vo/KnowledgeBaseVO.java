package com.yupi.aicodehelper.model.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class KnowledgeBaseVO {

    private Long id;

    private Long userId;

    private String name;

    private String description;

    private Integer status;

    private Integer documentCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
