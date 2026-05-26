package com.yupi.aicodehelper.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.aicodehelper.common.ErrorCode;
import com.yupi.aicodehelper.exception.BusinessException;
import com.yupi.aicodehelper.mapper.KnowledgeBaseMapper;
import com.yupi.aicodehelper.mapper.KnowledgeDocumentMapper;
import com.yupi.aicodehelper.mapper.KnowledgeSegmentMapper;
import com.yupi.aicodehelper.model.entity.KnowledgeBase;
import com.yupi.aicodehelper.model.entity.KnowledgeDocument;
import com.yupi.aicodehelper.model.entity.KnowledgeSegment;
import com.yupi.aicodehelper.service.KnowledgeVectorService;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class KnowledgeVectorServiceImpl implements KnowledgeVectorService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    private final KnowledgeSegmentMapper knowledgeSegmentMapper;

    private final ObjectMapper objectMapper;

    @Resource
    private EmbeddingModel qwenEmbeddingModel;

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

    public KnowledgeVectorServiceImpl(KnowledgeBaseMapper knowledgeBaseMapper,
                                      KnowledgeDocumentMapper knowledgeDocumentMapper,
                                      KnowledgeSegmentMapper knowledgeSegmentMapper,
                                      ObjectMapper objectMapper) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.knowledgeSegmentMapper = knowledgeSegmentMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer vectorizeDocument(Long userId, Long knowledgeBaseId, Long documentId) {
        validateUserId(userId);
        validateKnowledgeBaseId(knowledgeBaseId);
        validateDocumentId(documentId);

        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        validateOwnedKnowledgeBase(userId, knowledgeBase);

        KnowledgeDocument knowledgeDocument = knowledgeDocumentMapper.selectById(documentId);
        validateOwnedKnowledgeDocument(userId, knowledgeBaseId, knowledgeDocument);

        List<KnowledgeSegment> segmentList = knowledgeSegmentMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeSegment>()
                        .eq(KnowledgeSegment::getUserId, userId)
                        .eq(KnowledgeSegment::getKnowledgeBaseId, knowledgeBaseId)
                        .eq(KnowledgeSegment::getDocumentId, documentId)
                        .eq(KnowledgeSegment::getIsDelete, 0)
                        .orderByAsc(KnowledgeSegment::getSegmentIndex)
        );
        if ((knowledgeDocument.getSegmentCount() == null || knowledgeDocument.getSegmentCount() <= 0)
                && CollectionUtils.isEmpty(segmentList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前文档尚未完成解析");
        }
        if (CollectionUtils.isEmpty(segmentList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前文档没有可向量化的切片");
        }

        List<TextSegment> textSegmentList = new ArrayList<>(segmentList.size());
        List<String> metadataJsonList = new ArrayList<>(segmentList.size());
        for (KnowledgeSegment knowledgeSegment : segmentList) {
            Map<String, Object> metadataMap = buildMetadataMap(userId, knowledgeBaseId, knowledgeDocument, knowledgeSegment);
            Metadata metadata = Metadata.from(metadataMap);
            textSegmentList.add(TextSegment.from(knowledgeSegment.getContent(), metadata));
            metadataJsonList.add(writeMetadataJson(metadataMap));
        }

        Response<List<Embedding>> embeddingResponse = qwenEmbeddingModel.embedAll(textSegmentList);
        List<Embedding> embeddingList = embeddingResponse == null ? null : embeddingResponse.content();
        if (CollectionUtils.isEmpty(embeddingList) || embeddingList.size() != textSegmentList.size()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成文档向量失败");
        }

        List<String> vectorIdList = embeddingStore.addAll(embeddingList, textSegmentList);
        if (CollectionUtils.isEmpty(vectorIdList) || vectorIdList.size() != segmentList.size()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "写入向量库失败");
        }

        for (int i = 0; i < segmentList.size(); i++) {
            String vectorId = vectorIdList.get(i);
            if (!StringUtils.hasText(vectorId)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "向量 ID 生成失败");
            }
            KnowledgeSegment sourceSegment = segmentList.get(i);
            KnowledgeSegment updateSegment = new KnowledgeSegment();
            updateSegment.setId(sourceSegment.getId());
            updateSegment.setVectorId(vectorId);
            updateSegment.setMetadata(metadataJsonList.get(i));
            updateSegment.setStatus(2);
            int updatedRows = knowledgeSegmentMapper.updateById(updateSegment);
            if (updatedRows <= 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新切片向量信息失败");
            }
        }

        KnowledgeDocument updateDocument = new KnowledgeDocument();
        updateDocument.setId(documentId);
        updateDocument.setStatus(3);
        int updatedRows = knowledgeDocumentMapper.updateById(updateDocument);
        if (updatedRows <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新文档向量化状态失败");
        }
        return segmentList.size();
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }
    }

    private void validateKnowledgeBaseId(Long knowledgeBaseId) {
        if (knowledgeBaseId == null || knowledgeBaseId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "知识库 id 不能为空");
        }
    }

    private void validateDocumentId(Long documentId) {
        if (documentId == null || documentId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文档 id 不能为空");
        }
    }

    private void validateOwnedKnowledgeBase(Long userId, KnowledgeBase knowledgeBase) {
        if (knowledgeBase == null || Integer.valueOf(1).equals(knowledgeBase.getIsDelete())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "知识库不存在");
        }
        if (!knowledgeBase.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该知识库");
        }
    }

    private void validateOwnedKnowledgeDocument(Long userId, Long knowledgeBaseId, KnowledgeDocument knowledgeDocument) {
        if (knowledgeDocument == null || Integer.valueOf(1).equals(knowledgeDocument.getIsDelete())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文档不存在");
        }
        if (!knowledgeDocument.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该文档");
        }
        if (!knowledgeDocument.getKnowledgeBaseId().equals(knowledgeBaseId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文档不属于当前知识库");
        }
    }

    private Map<String, Object> buildMetadataMap(Long userId,
                                                 Long knowledgeBaseId,
                                                 KnowledgeDocument knowledgeDocument,
                                                 KnowledgeSegment knowledgeSegment) {
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("userId", String.valueOf(userId));
        metadata.put("knowledgeBaseId", String.valueOf(knowledgeBaseId));
        metadata.put("documentId", String.valueOf(knowledgeDocument.getId()));
        metadata.put("segmentId", String.valueOf(knowledgeSegment.getId()));
        metadata.put("fileName", knowledgeDocument.getFileName());
        metadata.put("fileType", knowledgeDocument.getFileType());
        metadata.put("segmentIndex", knowledgeSegment.getSegmentIndex());
        return metadata;
    }

    private String writeMetadataJson(Map<String, Object> metadataMap) {
        try {
            return objectMapper.writeValueAsString(metadataMap);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "序列化切片 metadata 失败");
        }
    }
}
