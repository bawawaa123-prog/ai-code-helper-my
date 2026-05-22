package com.yupi.aicodehelper.model.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class KnowledgeDocumentVO {

    private Long id;

    private Long userId;

    private Long knowledgeBaseId;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private Integer segmentCount;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
