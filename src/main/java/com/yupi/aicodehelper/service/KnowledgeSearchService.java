package com.yupi.aicodehelper.service;

import com.yupi.aicodehelper.model.dto.knowledge.KnowledgeSearchRequest;
import com.yupi.aicodehelper.model.vo.RagSourceVO;
import java.util.List;

public interface KnowledgeSearchService {

    List<RagSourceVO> searchKnowledgeBase(Long userId, Long knowledgeBaseId, KnowledgeSearchRequest request);
}
