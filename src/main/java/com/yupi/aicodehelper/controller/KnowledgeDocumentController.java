package com.yupi.aicodehelper.controller;

import com.yupi.aicodehelper.auth.LoginUserHolder;
import com.yupi.aicodehelper.common.BaseResponse;
import com.yupi.aicodehelper.common.ResultUtils;
import com.yupi.aicodehelper.model.entity.User;
import com.yupi.aicodehelper.model.vo.KnowledgeDocumentVO;
import com.yupi.aicodehelper.service.KnowledgeDocumentService;
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
}
