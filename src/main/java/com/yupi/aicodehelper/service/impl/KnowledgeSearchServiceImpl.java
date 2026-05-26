package com.yupi.aicodehelper.service.impl;

import com.yupi.aicodehelper.common.ErrorCode;
import com.yupi.aicodehelper.exception.BusinessException;
import com.yupi.aicodehelper.mapper.KnowledgeBaseMapper;
import com.yupi.aicodehelper.model.dto.knowledge.KnowledgeSearchRequest;
import com.yupi.aicodehelper.model.entity.KnowledgeBase;
import com.yupi.aicodehelper.model.vo.RagSourceVO;
import com.yupi.aicodehelper.service.KnowledgeSearchService;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.embedding.filter.logical.And;
import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class KnowledgeSearchServiceImpl implements KnowledgeSearchService {

    private static final int DEFAULT_MAX_RESULTS = 5;

    private static final int MAX_MAX_RESULTS = 10;

    private static final int INTERNAL_SEARCH_LIMIT = 20;

    private static final double DEFAULT_MIN_SCORE = 0.6D;

    private static final int MAX_QUERY_LENGTH = 1000;

    private static final String UNKNOWN_SOURCE_NAME = "未知来源";

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    @Resource
    private EmbeddingModel qwenEmbeddingModel;

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

    public KnowledgeSearchServiceImpl(KnowledgeBaseMapper knowledgeBaseMapper) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
    }

    @Override
    public List<RagSourceVO> searchKnowledgeBase(Long userId, Long knowledgeBaseId, KnowledgeSearchRequest request) {
        validateUserId(userId);
        validateKnowledgeBaseId(knowledgeBaseId);
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        validateOwnedKnowledgeBase(userId, knowledgeBase);

        String query = validateAndNormalizeQuery(request);
        int maxResults = resolveMaxResults(request);
        double minScore = resolveMinScore(request);

        Response<Embedding> embeddingResponse = qwenEmbeddingModel.embed(query);
        Embedding queryEmbedding = embeddingResponse == null ? null : embeddingResponse.content();
        if (queryEmbedding == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成检索向量失败");
        }

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(Math.max(maxResults, INTERNAL_SEARCH_LIMIT))
                .minScore(minScore)
                .filter(buildFilter(userId, knowledgeBaseId))
                .build();

        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> matches = searchResult == null ? null : searchResult.matches();
        if (CollectionUtils.isEmpty(matches)) {
            return Collections.emptyList();
        }

        return matches.stream()
                .filter(match -> match != null && match.embedded() != null)
                .filter(match -> belongsToCurrentKnowledgeBase(match.embedded(), userId, knowledgeBaseId))
                .map(this::toRagSourceVO)
                .filter(source -> StringUtils.hasText(source.getContent()))
                .limit(maxResults)
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

    private void validateOwnedKnowledgeBase(Long userId, KnowledgeBase knowledgeBase) {
        if (knowledgeBase == null || Integer.valueOf(1).equals(knowledgeBase.getIsDelete())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "知识库不存在");
        }
        if (!knowledgeBase.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该知识库");
        }
    }

    private String validateAndNormalizeQuery(KnowledgeSearchRequest request) {
        String query = request == null ? null : request.getQuery();
        if (!StringUtils.hasText(query)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "检索问题不能为空");
        }
        String trimmedQuery = query.trim();
        if (trimmedQuery.length() > MAX_QUERY_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "检索问题长度不能超过 1000");
        }
        return trimmedQuery;
    }

    private int resolveMaxResults(KnowledgeSearchRequest request) {
        Integer maxResults = request == null ? null : request.getMaxResults();
        if (maxResults == null || maxResults <= 0) {
            return DEFAULT_MAX_RESULTS;
        }
        if (maxResults > MAX_MAX_RESULTS) {
            return DEFAULT_MAX_RESULTS;
        }
        return maxResults;
    }

    private double resolveMinScore(KnowledgeSearchRequest request) {
        Double minScore = request == null ? null : request.getMinScore();
        if (minScore == null) {
            return DEFAULT_MIN_SCORE;
        }
        return minScore;
    }

    private Filter buildFilter(Long userId, Long knowledgeBaseId) {
        Filter userFilter = MetadataFilterBuilder.metadataKey("userId").isEqualTo(String.valueOf(userId));
        Filter knowledgeBaseFilter =
                MetadataFilterBuilder.metadataKey("knowledgeBaseId").isEqualTo(String.valueOf(knowledgeBaseId));
        return new And(userFilter, knowledgeBaseFilter);
    }

    private boolean belongsToCurrentKnowledgeBase(TextSegment textSegment, Long userId, Long knowledgeBaseId) {
        Metadata metadata = textSegment.metadata();
        if (metadata == null) {
            return false;
        }
        String metadataUserId = metadata.getString("userId");
        String metadataKnowledgeBaseId = metadata.getString("knowledgeBaseId");
        return String.valueOf(userId).equals(metadataUserId)
                && String.valueOf(knowledgeBaseId).equals(metadataKnowledgeBaseId);
    }

    private RagSourceVO toRagSourceVO(EmbeddingMatch<TextSegment> match) {
        TextSegment textSegment = match.embedded();
        Metadata metadata = textSegment.metadata();
        Map<String, Object> metadataMap = metadata == null ? null : metadata.toMap();

        RagSourceVO ragSourceVO = new RagSourceVO();
        String sourceName = metadata == null ? null : metadata.getString("fileName");
        ragSourceVO.setSourceName(StringUtils.hasText(sourceName) ? sourceName : UNKNOWN_SOURCE_NAME);
        ragSourceVO.setContent(textSegment.text());
        ragSourceVO.setScore(match.score());
        ragSourceVO.setMetadata(metadataMap == null || metadataMap.isEmpty() ? null : metadataMap);
        return ragSourceVO;
    }
}
