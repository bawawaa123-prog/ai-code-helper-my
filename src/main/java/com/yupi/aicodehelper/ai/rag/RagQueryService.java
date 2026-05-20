package com.yupi.aicodehelper.ai.rag;

import com.yupi.aicodehelper.model.vo.RagSourceVO;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class RagQueryService {

    private final ObjectProvider<ContentRetriever> contentRetrieverProvider;

    public RagQueryService(ObjectProvider<ContentRetriever> contentRetrieverProvider) {
        this.contentRetrieverProvider = contentRetrieverProvider;
    }

    public List<RagSourceVO> querySources(String query) {
        if (!StringUtils.hasText(query)) {
            return Collections.emptyList();
        }
        try {
            ContentRetriever contentRetriever = contentRetrieverProvider.getIfAvailable();
            if (contentRetriever == null) {
                return Collections.emptyList();
            }
            List<Content> contents = contentRetriever.retrieve(Query.from(query));
            if (contents == null || contents.isEmpty()) {
                return Collections.emptyList();
            }
            return contents.stream()
                    .map(this::toRagSourceVO)
                    .toList();
        } catch (Exception e) {
            log.warn("RAG query failed, return empty source list. query={}", query, e);
            return Collections.emptyList();
        }
    }

    private RagSourceVO toRagSourceVO(Content content) {
        TextSegment textSegment = content.textSegment();
        Metadata metadata = textSegment.metadata();
        String sourceName = extractSourceName(metadata, textSegment.text());

        RagSourceVO ragSourceVO = new RagSourceVO();
        ragSourceVO.setSourceName(sourceName);
        ragSourceVO.setContent(extractContent(textSegment.text(), sourceName));
        ragSourceVO.setScore(extractScore(content.metadata()));
        ragSourceVO.setMetadata(metadata == null || metadata.toMap().isEmpty() ? null : metadata.toMap());
        return ragSourceVO;
    }

    private String extractSourceName(Metadata metadata, String contentText) {
        if (metadata != null) {
            String fileName = metadata.getString(RagConfig.SOURCE_FILE_NAME_METADATA_KEY);
            if (StringUtils.hasText(fileName)) {
                return fileName;
            }
        }
        if (!StringUtils.hasText(contentText)) {
            return null;
        }
        int lineBreakIndex = contentText.indexOf('\n');
        if (lineBreakIndex <= 0) {
            return null;
        }
        String firstLine = contentText.substring(0, lineBreakIndex).trim();
        return StringUtils.hasText(firstLine) ? firstLine : null;
    }

    private String extractContent(String contentText, String sourceName) {
        if (!StringUtils.hasText(contentText)) {
            return contentText;
        }
        if (!StringUtils.hasText(sourceName)) {
            return contentText;
        }
        String prefix = sourceName + "\n";
        if (contentText.startsWith(prefix)) {
            return contentText.substring(prefix.length());
        }
        return contentText;
    }

    private Double extractScore(Map<ContentMetadata, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        Object score = metadata.get(ContentMetadata.SCORE);
        if (!(score instanceof Number)) {
            score = metadata.get(ContentMetadata.RERANKED_SCORE);
        }
        return score instanceof Number ? ((Number) score).doubleValue() : null;
    }
}
