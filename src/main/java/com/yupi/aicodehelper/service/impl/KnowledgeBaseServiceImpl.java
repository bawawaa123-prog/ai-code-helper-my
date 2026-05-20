package com.yupi.aicodehelper.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.aicodehelper.common.ErrorCode;
import com.yupi.aicodehelper.exception.BusinessException;
import com.yupi.aicodehelper.mapper.KnowledgeBaseMapper;
import com.yupi.aicodehelper.model.dto.knowledge.KnowledgeBaseCreateRequest;
import com.yupi.aicodehelper.model.dto.knowledge.KnowledgeBaseUpdateRequest;
import com.yupi.aicodehelper.model.entity.KnowledgeBase;
import com.yupi.aicodehelper.model.vo.KnowledgeBaseVO;
import com.yupi.aicodehelper.service.KnowledgeBaseService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase>
        implements KnowledgeBaseService {

    private static final int MAX_NAME_LENGTH = 64;

    private static final int MAX_DESCRIPTION_LENGTH = 512;

    @Override
    public Long createKnowledgeBase(Long userId, KnowledgeBaseCreateRequest request) {
        validateUserId(userId);
        String name = validateAndNormalizeName(request == null ? null : request.getName());
        String description = validateAndNormalizeDescription(request == null ? null : request.getDescription());

        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setUserId(userId);
        knowledgeBase.setName(name);
        knowledgeBase.setDescription(description);
        knowledgeBase.setStatus(1);
        boolean saved = save(knowledgeBase);
        if (!saved || knowledgeBase.getId() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建知识库失败");
        }
        return knowledgeBase.getId();
    }

    @Override
    public List<KnowledgeBaseVO> listMyKnowledgeBases(Long userId) {
        validateUserId(userId);
        return lambdaQuery()
                .eq(KnowledgeBase::getUserId, userId)
                .eq(KnowledgeBase::getIsDelete, 0)
                .orderByDesc(KnowledgeBase::getUpdateTime)
                .list()
                .stream()
                .map(this::toKnowledgeBaseVO)
                .toList();
    }

    @Override
    public void updateKnowledgeBase(Long userId, Long knowledgeBaseId, KnowledgeBaseUpdateRequest request) {
        validateUserId(userId);
        if (knowledgeBaseId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "知识库 id 不能为空");
        }
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        KnowledgeBase knowledgeBase = getById(knowledgeBaseId);
        validateOwnedKnowledgeBase(userId, knowledgeBase);

        String name = validateAndNormalizeName(request.getName());
        String description = validateAndNormalizeDescription(request.getDescription());

        var updateChain = lambdaUpdate()
                .eq(KnowledgeBase::getId, knowledgeBaseId)
                .eq(KnowledgeBase::getUserId, userId)
                .eq(KnowledgeBase::getIsDelete, 0)
                .set(KnowledgeBase::getName, name)
                .set(KnowledgeBase::getDescription, description);
        if (request.getStatus() != null) {
            updateChain.set(KnowledgeBase::getStatus, request.getStatus());
        }
        boolean updated = updateChain.update();
        if (!updated) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新知识库失败");
        }
    }

    @Override
    public void deleteKnowledgeBase(Long userId, Long knowledgeBaseId) {
        validateUserId(userId);
        if (knowledgeBaseId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "知识库 id 不能为空");
        }

        KnowledgeBase knowledgeBase = getById(knowledgeBaseId);
        validateOwnedKnowledgeBase(userId, knowledgeBase);

        boolean updated = lambdaUpdate()
                .eq(KnowledgeBase::getId, knowledgeBaseId)
                .eq(KnowledgeBase::getUserId, userId)
                .eq(KnowledgeBase::getIsDelete, 0)
                .set(KnowledgeBase::getIsDelete, 1)
                .update();
        if (!updated) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除知识库失败");
        }
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
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

    private String validateAndNormalizeName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "知识库名称不能为空");
        }
        String trimmedName = name.trim();
        if (trimmedName.length() > MAX_NAME_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "知识库名称长度不能超过 64");
        }
        return trimmedName;
    }

    private String validateAndNormalizeDescription(String description) {
        if (!StringUtils.hasText(description)) {
            return null;
        }
        String trimmedDescription = description.trim();
        if (trimmedDescription.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "知识库描述长度不能超过 512");
        }
        return trimmedDescription;
    }

    private KnowledgeBaseVO toKnowledgeBaseVO(KnowledgeBase knowledgeBase) {
        KnowledgeBaseVO knowledgeBaseVO = new KnowledgeBaseVO();
        knowledgeBaseVO.setId(knowledgeBase.getId());
        knowledgeBaseVO.setUserId(knowledgeBase.getUserId());
        knowledgeBaseVO.setName(knowledgeBase.getName());
        knowledgeBaseVO.setDescription(knowledgeBase.getDescription());
        knowledgeBaseVO.setStatus(knowledgeBase.getStatus());
        knowledgeBaseVO.setDocumentCount(0);
        knowledgeBaseVO.setCreateTime(knowledgeBase.getCreateTime());
        knowledgeBaseVO.setUpdateTime(knowledgeBase.getUpdateTime());
        return knowledgeBaseVO;
    }
}
