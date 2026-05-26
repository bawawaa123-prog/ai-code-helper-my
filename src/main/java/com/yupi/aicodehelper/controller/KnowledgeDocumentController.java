package com.yupi.aicodehelper.controller;

import com.yupi.aicodehelper.auth.LoginUserHolder;
import com.yupi.aicodehelper.common.BaseResponse;
import com.yupi.aicodehelper.common.ResultUtils;
import com.yupi.aicodehelper.model.entity.User;
import com.yupi.aicodehelper.model.vo.KnowledgeDocumentVO;
import com.yupi.aicodehelper.model.vo.KnowledgeSegmentVO;
import com.yupi.aicodehelper.service.KnowledgeDocumentService;
import com.yupi.aicodehelper.service.KnowledgeSegmentService;
import com.yupi.aicodehelper.service.KnowledgeVectorService;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/knowledge/base/{knowledgeBaseId}/document")
public class KnowledgeDocumentController {

    @Resource
    private KnowledgeDocumentService knowledgeDocumentService;

    @Resource
    private KnowledgeSegmentService knowledgeSegmentService;

    @Resource
    private KnowledgeVectorService knowledgeVectorService;

    @PostMapping("/upload")
    public BaseResponse<Long> uploadDocument(@PathVariable Long knowledgeBaseId,
                                             @RequestParam("file") MultipartFile file) {
        User loginUser = LoginUserHolder.get();
        Long documentId = knowledgeDocumentService.uploadDocument(loginUser.getId(), knowledgeBaseId, file);
        return ResultUtils.success(documentId);
    }

    @GetMapping("/list")
    public BaseResponse<List<KnowledgeDocumentVO>> listDocuments(@PathVariable Long knowledgeBaseId) {
        User loginUser = LoginUserHolder.get();
        List<KnowledgeDocumentVO> documentList =
                knowledgeDocumentService.listDocuments(loginUser.getId(), knowledgeBaseId);
        return ResultUtils.success(documentList);
    }

    @PostMapping("/{documentId}/parse")
    public BaseResponse<Integer> parseDocument(@PathVariable Long knowledgeBaseId, @PathVariable Long documentId) {
        User loginUser = LoginUserHolder.get();
        Integer segmentCount =
                knowledgeSegmentService.parseAndSaveSegments(loginUser.getId(), knowledgeBaseId, documentId);
        return ResultUtils.success(segmentCount);
    }

    @PostMapping("/{documentId}/vectorize")
    public BaseResponse<Integer> vectorizeDocument(@PathVariable Long knowledgeBaseId,
                                                   @PathVariable Long documentId) {
        User loginUser = LoginUserHolder.get();
        Integer vectorizedCount =
                knowledgeVectorService.vectorizeDocument(loginUser.getId(), knowledgeBaseId, documentId);
        return ResultUtils.success(vectorizedCount);
    }

    @GetMapping("/{documentId}/segment/list")
    public BaseResponse<List<KnowledgeSegmentVO>> listSegments(@PathVariable Long knowledgeBaseId,
                                                               @PathVariable Long documentId) {
        User loginUser = LoginUserHolder.get();
        List<KnowledgeSegmentVO> segmentList =
                knowledgeSegmentService.listSegments(loginUser.getId(), knowledgeBaseId, documentId);
        return ResultUtils.success(segmentList);
    }
}
