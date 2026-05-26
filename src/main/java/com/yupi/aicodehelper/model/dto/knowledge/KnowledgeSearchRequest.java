package com.yupi.aicodehelper.model.dto.knowledge;

import lombok.Data;

@Data
public class KnowledgeSearchRequest {

    private String query;

    private Integer maxResults;

    private Double minScore;
}
