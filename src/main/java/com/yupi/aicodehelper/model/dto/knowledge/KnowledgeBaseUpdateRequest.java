package com.yupi.aicodehelper.model.dto.knowledge;

import lombok.Data;

@Data
public class KnowledgeBaseUpdateRequest {

    private String name;

    private String description;

    private Integer status;
}
