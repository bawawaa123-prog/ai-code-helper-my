package com.yupi.aicodehelper.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.aicodehelper.common.ErrorCode;
import com.yupi.aicodehelper.exception.BusinessException;
import com.yupi.aicodehelper.mapper.KnowledgeBaseMapper;
import com.yupi.aicodehelper.mapper.KnowledgeDocumentMapper;
import com.yupi.aicodehelper.mapper.KnowledgeSegmentMapper;
import com.yupi.aicodehelper.model.entity.KnowledgeBase;
import com.yupi.aicodehelper.model.entity.KnowledgeDocument;
import com.yupi.aicodehelper.model.entity.KnowledgeSegment;
import com.yupi.aicodehelper.model.vo.KnowledgeDocumentVO;
import com.yupi.aicodehelper.service.KnowledgeDocumentService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class KnowledgeDocumentServiceImpl extends ServiceImpl<KnowledgeDocumentMapper, KnowledgeDocument>
        implements KnowledgeDocumentService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    private static final Path BASE_UPLOAD_DIR = Paths.get("data", "knowledge");

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeSegmentMapper knowledgeSegmentMapper;

    public KnowledgeDocumentServiceImpl(KnowledgeBaseMapper knowledgeBaseMapper,
                                        KnowledgeSegmentMapper knowledgeSegmentMapper) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.knowledgeSegmentMapper = knowledgeSegmentMapper;
    }

    @Override
    public Long uploadDocument(Long userId, Long knowledgeBaseId, MultipartFile file) {
        validateUserId(userId);
        validateKnowledgeBaseId(knowledgeBaseId);
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        validateOwnedKnowledgeBase(userId, knowledgeBase);
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String safeDisplayName = sanitizeDisplayFileName(originalFilename);
        String fileType = resolveFileType(safeDisplayName);
        Path targetDirectory = BASE_UPLOAD_DIR.resolve(String.valueOf(userId)).resolve(String.valueOf(knowledgeBaseId));

        try {
            Files.createDirectories(targetDirectory);
            String storedFileName = UUID.randomUUID() + "." + fileType;
            Path targetFile = targetDirectory.resolve(storedFileName).normalize();
            if (!targetFile.startsWith(targetDirectory.normalize())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法文件路径");
            }
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

            KnowledgeDocument knowledgeDocument = new KnowledgeDocument();
            knowledgeDocument.setUserId(userId);
            knowledgeDocument.setKnowledgeBaseId(knowledgeBaseId);
            knowledgeDocument.setFileName(safeDisplayName);
            knowledgeDocument.setFileType(fileType);
            knowledgeDocument.setFilePath(targetFile.toString());
            knowledgeDocument.setFileSize(file.getSize());
            knowledgeDocument.setSegmentCount(0);
            knowledgeDocument.setStatus(1);
            boolean saved = save(knowledgeDocument);
            if (!saved || knowledgeDocument.getId() == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存文档信息失败");
            }
            return knowledgeDocument.getId();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存文档文件失败");
        }
    }

    @Override
    public List<KnowledgeDocumentVO> listDocuments(Long userId, Long knowledgeBaseId) {
        validateUserId(userId);
        validateKnowledgeBaseId(knowledgeBaseId);
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        validateOwnedKnowledgeBase(userId, knowledgeBase);
        return lambdaQuery()
                .eq(KnowledgeDocument::getUserId, userId)
                .eq(KnowledgeDocument::getKnowledgeBaseId, knowledgeBaseId)
                .eq(KnowledgeDocument::getIsDelete, 0)
                .orderByDesc(KnowledgeDocument::getUpdateTime)
                .list()
                .stream()
                .map(this::toKnowledgeDocumentVO)
                .toList();
    }

    @Override
    public void deleteDocument(Long userId, Long knowledgeBaseId, Long documentId) {
        validateUserId(userId);
        validateKnowledgeBaseId(knowledgeBaseId);
        validateDocumentId(documentId);

        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        validateOwnedKnowledgeBase(userId, knowledgeBase);

        KnowledgeDocument knowledgeDocument = baseMapper.selectById(documentId);
        validateOwnedKnowledgeDocument(userId, knowledgeBaseId, knowledgeDocument);

        boolean documentDeleted = lambdaUpdate()
                .eq(KnowledgeDocument::getId, documentId)
                .eq(KnowledgeDocument::getUserId, userId)
                .eq(KnowledgeDocument::getKnowledgeBaseId, knowledgeBaseId)
                .eq(KnowledgeDocument::getIsDelete, 0)
                .set(KnowledgeDocument::getIsDelete, 1)
                .update();
        if (!documentDeleted) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除文档失败");
        }

        KnowledgeSegment knowledgeSegment = new KnowledgeSegment();
        knowledgeSegment.setIsDelete(1);
        knowledgeSegmentMapper.update(
                knowledgeSegment,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<KnowledgeSegment>()
                        .eq(KnowledgeSegment::getUserId, userId)
                        .eq(KnowledgeSegment::getKnowledgeBaseId, knowledgeBaseId)
                        .eq(KnowledgeSegment::getDocumentId, documentId)
                        .eq(KnowledgeSegment::getIsDelete, 0)
        );
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
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限删除该文档");
        }
        if (!knowledgeDocument.getKnowledgeBaseId().equals(knowledgeBaseId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文档不属于当前知识库");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        String safeDisplayName = sanitizeDisplayFileName(originalFilename);
        resolveFileType(safeDisplayName);
        if (file.getSize() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 5MB");
        }
    }

    private String sanitizeDisplayFileName(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名不能为空");
        }
        String fileName = Paths.get(originalFilename).getFileName().toString().trim();
        fileName = fileName.replace("\\", "_").replace("/", "_");
        fileName = fileName.replace("..", "_");
        if (!StringUtils.hasText(fileName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名不能为空");
        }
        return fileName;
    }

    private String resolveFileType(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持上传 md 或 txt 文件");
        }
        String extension = fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (!"md".equals(extension) && !"txt".equals(extension)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持上传 md 或 txt 文件");
        }
        return extension;
    }

    private KnowledgeDocumentVO toKnowledgeDocumentVO(KnowledgeDocument knowledgeDocument) {
        KnowledgeDocumentVO knowledgeDocumentVO = new KnowledgeDocumentVO();
        knowledgeDocumentVO.setId(knowledgeDocument.getId());
        knowledgeDocumentVO.setUserId(knowledgeDocument.getUserId());
        knowledgeDocumentVO.setKnowledgeBaseId(knowledgeDocument.getKnowledgeBaseId());
        knowledgeDocumentVO.setFileName(knowledgeDocument.getFileName());
        knowledgeDocumentVO.setFileType(knowledgeDocument.getFileType());
        knowledgeDocumentVO.setFileSize(knowledgeDocument.getFileSize());
        knowledgeDocumentVO.setSegmentCount(knowledgeDocument.getSegmentCount());
        knowledgeDocumentVO.setStatus(knowledgeDocument.getStatus());
        knowledgeDocumentVO.setCreateTime(knowledgeDocument.getCreateTime());
        knowledgeDocumentVO.setUpdateTime(knowledgeDocument.getUpdateTime());
        return knowledgeDocumentVO;
    }
}
