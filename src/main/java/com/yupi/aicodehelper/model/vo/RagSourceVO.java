package com.yupi.aicodehelper.model.vo;

import java.util.Map;
import lombok.Data;

@Data
public class RagSourceVO {

    private String sourceName;

    private String content;

    private Double score;

    private Map<String, Object> metadata;
}
