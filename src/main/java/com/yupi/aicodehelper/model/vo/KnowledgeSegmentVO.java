package com.yupi.aicodehelper.model.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class KnowledgeSegmentVO {

    private Long id;

    private Long userId;

    private Long knowledgeBaseId;

    private Long documentId;

    private Integer segmentIndex;

    private String content;

    private Integer tokenCount;

    private String metadata;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
