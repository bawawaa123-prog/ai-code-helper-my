package com.yupi.aicodehelper.controller;

import com.yupi.aicodehelper.auth.LoginUserHolder;
import com.yupi.aicodehelper.common.BaseResponse;
import com.yupi.aicodehelper.common.ResultUtils;
import com.yupi.aicodehelper.model.dto.knowledge.KnowledgeBaseCreateRequest;
import com.yupi.aicodehelper.model.dto.knowledge.KnowledgeBaseUpdateRequest;
import com.yupi.aicodehelper.model.entity.User;
import com.yupi.aicodehelper.model.vo.KnowledgeBaseVO;
import com.yupi.aicodehelper.service.KnowledgeBaseService;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/knowledge/base")
public class KnowledgeBaseController {

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @PostMapping
    public BaseResponse<Long> createKnowledgeBase(@RequestBody(required = false) KnowledgeBaseCreateRequest request) {
        User loginUser = LoginUserHolder.get();
        Long knowledgeBaseId = knowledgeBaseService.createKnowledgeBase(loginUser.getId(), request);
        return ResultUtils.success(knowledgeBaseId);
    }

    @GetMapping("/list")
    public BaseResponse<List<KnowledgeBaseVO>> listMyKnowledgeBases() {
        User loginUser = LoginUserHolder.get();
        List<KnowledgeBaseVO> knowledgeBaseList = knowledgeBaseService.listMyKnowledgeBases(loginUser.getId());
        return ResultUtils.success(knowledgeBaseList);
    }

    @PutMapping("/{knowledgeBaseId}")
    public BaseResponse<Boolean> updateKnowledgeBase(@PathVariable Long knowledgeBaseId,
                                                     @RequestBody(required = false) KnowledgeBaseUpdateRequest request) {
        User loginUser = LoginUserHolder.get();
        knowledgeBaseService.updateKnowledgeBase(loginUser.getId(), knowledgeBaseId, request);
        return ResultUtils.success(Boolean.TRUE);
    }

    @DeleteMapping("/{knowledgeBaseId}")
    public BaseResponse<Boolean> deleteKnowledgeBase(@PathVariable Long knowledgeBaseId) {
        User loginUser = LoginUserHolder.get();
        knowledgeBaseService.deleteKnowledgeBase(loginUser.getId(), knowledgeBaseId);
        return ResultUtils.success(Boolean.TRUE);
    }
}
