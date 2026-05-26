package com.yupi.aicodehelper.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import com.yupi.aicodehelper.model.vo.KnowledgeSegmentVO;
import com.yupi.aicodehelper.service.KnowledgeSegmentService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class KnowledgeSegmentServiceImpl extends ServiceImpl<KnowledgeSegmentMapper, KnowledgeSegment>
        implements KnowledgeSegmentService {

    private static final int TARGET_SEGMENT_LENGTH = 1000;

    private static final int MAX_SEGMENT_LENGTH = 1200;

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    private final ObjectMapper objectMapper;

    public KnowledgeSegmentServiceImpl(KnowledgeBaseMapper knowledgeBaseMapper,
                                       KnowledgeDocumentMapper knowledgeDocumentMapper,
                                       ObjectMapper objectMapper) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Integer parseAndSaveSegments(Long userId, Long knowledgeBaseId, Long documentId) {
        validateUserId(userId);
        validateKnowledgeBaseId(knowledgeBaseId);
        validateDocumentId(documentId);

        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        validateOwnedKnowledgeBase(userId, knowledgeBase);

        KnowledgeDocument knowledgeDocument = knowledgeDocumentMapper.selectById(documentId);
        validateOwnedKnowledgeDocument(userId, knowledgeBaseId, knowledgeDocument);
        validateDocumentFileType(knowledgeDocument);

        String content = readDocumentContent(knowledgeDocument);
        List<String> segments = splitToSegments(content);
        if (segments.isEmpty()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文档解析后没有有效切片");
        }

        lambdaUpdate()
                .eq(KnowledgeSegment::getUserId, userId)
                .eq(KnowledgeSegment::getKnowledgeBaseId, knowledgeBaseId)
                .eq(KnowledgeSegment::getDocumentId, documentId)
                .eq(KnowledgeSegment::getIsDelete, 0)
                .set(KnowledgeSegment::getIsDelete, 1)
                .update();

        List<KnowledgeSegment> knowledgeSegmentList = new ArrayList<>(segments.size());
        for (int i = 0; i < segments.size(); i++) {
            String segmentContent = segments.get(i);
            KnowledgeSegment knowledgeSegment = new KnowledgeSegment();
            knowledgeSegment.setUserId(userId);
            knowledgeSegment.setKnowledgeBaseId(knowledgeBaseId);
            knowledgeSegment.setDocumentId(documentId);
            knowledgeSegment.setSegmentIndex(i);
            knowledgeSegment.setContent(segmentContent);
            knowledgeSegment.setTokenCount(segmentContent.length());
            knowledgeSegment.setVectorId(null);
            knowledgeSegment.setMetadata(buildMetadata(knowledgeDocument, i));
            knowledgeSegment.setStatus(1);
            knowledgeSegmentList.add(knowledgeSegment);
        }

        boolean saved = saveBatch(knowledgeSegmentList);
        if (!saved) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存文档切片失败");
        }

        KnowledgeDocument updateDocument = new KnowledgeDocument();
        updateDocument.setId(documentId);
        updateDocument.setSegmentCount(knowledgeSegmentList.size());
        updateDocument.setStatus(2);
        int updatedRows = knowledgeDocumentMapper.updateById(updateDocument);
        if (updatedRows <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新文档切片信息失败");
        }
        return knowledgeSegmentList.size();
    }

    @Override
    public List<KnowledgeSegmentVO> listSegments(Long userId, Long knowledgeBaseId, Long documentId) {
        validateUserId(userId);
        validateKnowledgeBaseId(knowledgeBaseId);
        validateDocumentId(documentId);

        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        validateOwnedKnowledgeBase(userId, knowledgeBase);

        KnowledgeDocument knowledgeDocument = knowledgeDocumentMapper.selectById(documentId);
        validateOwnedKnowledgeDocument(userId, knowledgeBaseId, knowledgeDocument);

        return lambdaQuery()
                .eq(KnowledgeSegment::getUserId, userId)
                .eq(KnowledgeSegment::getKnowledgeBaseId, knowledgeBaseId)
                .eq(KnowledgeSegment::getDocumentId, documentId)
                .eq(KnowledgeSegment::getIsDelete, 0)
                .orderByAsc(KnowledgeSegment::getSegmentIndex)
                .list()
                .stream()
                .map(this::toKnowledgeSegmentVO)
                .toList();
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
        if (knowledgeBase == null || knowledgeBase.getIsDelete() != null && knowledgeBase.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "知识库不存在");
        }
        if (!knowledgeBase.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该知识库");
        }
    }

    private void validateOwnedKnowledgeDocument(Long userId, Long knowledgeBaseId, KnowledgeDocument knowledgeDocument) {
        if (knowledgeDocument == null || knowledgeDocument.getIsDelete() != null && knowledgeDocument.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文档不存在");
        }
        if (!knowledgeDocument.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该文档");
        }
        if (!knowledgeDocument.getKnowledgeBaseId().equals(knowledgeBaseId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文档不属于当前知识库");
        }
    }

    private void validateDocumentFileType(KnowledgeDocument knowledgeDocument) {
        String fileType = knowledgeDocument.getFileType();
        if (!"md".equalsIgnoreCase(fileType) && !"txt".equalsIgnoreCase(fileType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前仅支持解析 md 或 txt 文档");
        }
    }

    private String readDocumentContent(KnowledgeDocument knowledgeDocument) {
        if (!StringUtils.hasText(knowledgeDocument.getFilePath())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文档文件路径不存在");
        }
        Path filePath = Paths.get(knowledgeDocument.getFilePath()).normalize();
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文档原始文件不存在");
        }
        try {
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取文档文件失败");
        }
    }

    private List<String> splitToSegments(String rawText) {
        if (!StringUtils.hasText(rawText)) {
            return List.of();
        }
        String normalizedText = rawText.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (!StringUtils.hasText(normalizedText)) {
            return List.of();
        }

        String[] paragraphArray = normalizedText.split("\\n\\s*\\n+");
        List<String> paragraphList = new ArrayList<>();
        for (String paragraph : paragraphArray) {
            if (!StringUtils.hasText(paragraph)) {
                continue;
            }
            String cleanedParagraph = paragraph
                    .replaceAll("[ \\t]+", " ")
                    .replaceAll("\\n{3,}", "\n\n")
                    .trim();
            if (StringUtils.hasText(cleanedParagraph)) {
                paragraphList.add(cleanedParagraph);
            }
        }
        if (paragraphList.isEmpty()) {
            return List.of();
        }

        List<String> segments = new ArrayList<>();
        StringBuilder currentSegment = new StringBuilder();
        for (String paragraph : paragraphList) {
            if (paragraph.length() > MAX_SEGMENT_LENGTH) {
                flushCurrentSegment(segments, currentSegment);
                segments.addAll(splitLongParagraph(paragraph));
                continue;
            }

            if (currentSegment.length() == 0) {
                currentSegment.append(paragraph);
                continue;
            }

            int nextLength = currentSegment.length() + 2 + paragraph.length();
            if (nextLength <= TARGET_SEGMENT_LENGTH || currentSegment.length() < 800) {
                currentSegment.append("\n\n").append(paragraph);
            } else {
                flushCurrentSegment(segments, currentSegment);
                currentSegment.append(paragraph);
            }
        }
        flushCurrentSegment(segments, currentSegment);
        return segments.stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private List<String> splitLongParagraph(String paragraph) {
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < paragraph.length()) {
            int end = Math.min(start + TARGET_SEGMENT_LENGTH, paragraph.length());
            if (end < paragraph.length()) {
                int breakIndex = paragraph.lastIndexOf('\n', end);
                if (breakIndex <= start) {
                    breakIndex = paragraph.lastIndexOf(' ', end);
                }
                if (breakIndex > start + 400) {
                    end = breakIndex;
                }
            }
            String chunk = paragraph.substring(start, end).trim();
            if (StringUtils.hasText(chunk)) {
                result.add(chunk);
            }
            start = end;
            while (start < paragraph.length() && Character.isWhitespace(paragraph.charAt(start))) {
                start++;
            }
        }
        return result;
    }

    private void flushCurrentSegment(List<String> segments, StringBuilder currentSegment) {
        if (currentSegment.length() == 0) {
            return;
        }
        String value = currentSegment.toString().trim();
        if (StringUtils.hasText(value)) {
            segments.add(value);
        }
        currentSegment.setLength(0);
    }

    private String buildMetadata(KnowledgeDocument knowledgeDocument, int segmentIndex) {
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("fileName", knowledgeDocument.getFileName());
        metadata.put("fileType", knowledgeDocument.getFileType());
        metadata.put("segmentIndex", segmentIndex);
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return "{\"fileName\":\"" + escapeJson(knowledgeDocument.getFileName())
                    + "\",\"fileType\":\"" + escapeJson(knowledgeDocument.getFileType())
                    + "\",\"segmentIndex\":" + segmentIndex + "}";
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private KnowledgeSegmentVO toKnowledgeSegmentVO(KnowledgeSegment knowledgeSegment) {
        KnowledgeSegmentVO knowledgeSegmentVO = new KnowledgeSegmentVO();
        knowledgeSegmentVO.setId(knowledgeSegment.getId());
        knowledgeSegmentVO.setUserId(knowledgeSegment.getUserId());
        knowledgeSegmentVO.setKnowledgeBaseId(knowledgeSegment.getKnowledgeBaseId());
        knowledgeSegmentVO.setDocumentId(knowledgeSegment.getDocumentId());
        knowledgeSegmentVO.setSegmentIndex(knowledgeSegment.getSegmentIndex());
        knowledgeSegmentVO.setContent(knowledgeSegment.getContent());
        knowledgeSegmentVO.setTokenCount(knowledgeSegment.getTokenCount());
        knowledgeSegmentVO.setMetadata(knowledgeSegment.getMetadata());
        knowledgeSegmentVO.setStatus(knowledgeSegment.getStatus());
        knowledgeSegmentVO.setCreateTime(knowledgeSegment.getCreateTime());
        knowledgeSegmentVO.setUpdateTime(knowledgeSegment.getUpdateTime());
        return knowledgeSegmentVO;
    }
}
