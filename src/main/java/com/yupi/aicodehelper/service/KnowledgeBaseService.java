package com.yupi.aicodehelper.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.aicodehelper.model.dto.knowledge.KnowledgeBaseCreateRequest;
import com.yupi.aicodehelper.model.dto.knowledge.KnowledgeBaseUpdateRequest;
import com.yupi.aicodehelper.model.entity.KnowledgeBase;
import com.yupi.aicodehelper.model.vo.KnowledgeBaseVO;
import java.util.List;

public interface KnowledgeBaseService extends IService<KnowledgeBase> {

    Long createKnowledgeBase(Long userId, KnowledgeBaseCreateRequest request);

    List<KnowledgeBaseVO> listMyKnowledgeBases(Long userId);

    void updateKnowledgeBase(Long userId, Long knowledgeBaseId, KnowledgeBaseUpdateRequest request);

    void deleteKnowledgeBase(Long userId, Long knowledgeBaseId);
}
