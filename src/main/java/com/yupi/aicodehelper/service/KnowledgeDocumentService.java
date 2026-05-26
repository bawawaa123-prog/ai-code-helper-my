package com.yupi.aicodehelper.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.aicodehelper.model.entity.KnowledgeDocument;
import com.yupi.aicodehelper.model.vo.KnowledgeDocumentVO;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface KnowledgeDocumentService extends IService<KnowledgeDocument> {

    Long uploadDocument(Long userId, Long knowledgeBaseId, MultipartFile file);

    List<KnowledgeDocumentVO> listDocuments(Long userId, Long knowledgeBaseId);

    void deleteDocument(Long userId, Long knowledgeBaseId, Long documentId);
}
