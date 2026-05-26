package com.yupi.aicodehelper.controller;

import com.yupi.aicodehelper.auth.LoginUserHolder;
import com.yupi.aicodehelper.common.BaseResponse;
import com.yupi.aicodehelper.common.ResultUtils;
import com.yupi.aicodehelper.model.dto.knowledge.KnowledgeSearchRequest;
import com.yupi.aicodehelper.model.entity.User;
import com.yupi.aicodehelper.model.vo.RagSourceVO;
import com.yupi.aicodehelper.service.KnowledgeSearchService;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/knowledge/base/{knowledgeBaseId}")
public class KnowledgeSearchController {

    @Resource
    private KnowledgeSearchService knowledgeSearchService;

    @PostMapping("/search")
    public BaseResponse<List<RagSourceVO>> searchKnowledgeBase(@PathVariable Long knowledgeBaseId,
                                                               @RequestBody(required = false) KnowledgeSearchRequest request) {
        User loginUser = LoginUserHolder.get();
        List<RagSourceVO> sourceList =
                knowledgeSearchService.searchKnowledgeBase(loginUser.getId(), knowledgeBaseId, request);
        return ResultUtils.success(sourceList);
    }
}
