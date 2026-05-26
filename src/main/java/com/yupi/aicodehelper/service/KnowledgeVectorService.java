package com.yupi.aicodehelper.service;

public interface KnowledgeVectorService {

    Integer vectorizeDocument(Long userId, Long knowledgeBaseId, Long documentId);
}
