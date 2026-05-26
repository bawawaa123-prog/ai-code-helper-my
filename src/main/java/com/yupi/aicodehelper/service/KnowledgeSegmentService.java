package com.yupi.aicodehelper.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.aicodehelper.model.entity.KnowledgeSegment;
import com.yupi.aicodehelper.model.vo.KnowledgeSegmentVO;
import java.util.List;

public interface KnowledgeSegmentService extends IService<KnowledgeSegment> {

    Integer parseAndSaveSegments(Long userId, Long knowledgeBaseId, Long documentId);

    List<KnowledgeSegmentVO> listSegments(Long userId, Long knowledgeBaseId, Long documentId);
}
