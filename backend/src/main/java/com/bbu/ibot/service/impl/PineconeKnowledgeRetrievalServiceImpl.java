package com.bbu.ibot.service.impl;

import com.bbu.ibot.model.vo.KnowledgeRetrievalResult;
import com.bbu.ibot.model.vo.RetrievedSegment;
import com.bbu.ibot.service.KnowledgeRetrievalService;
import com.bbu.ibot.service.SystemSettingsService;
import com.bbu.ibot.store.PineconeEmbeddingStoreFactory;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class PineconeKnowledgeRetrievalServiceImpl implements KnowledgeRetrievalService {

    private final EmbeddingModel embeddingModel;
    private final PineconeEmbeddingStoreFactory pineconeEmbeddingStoreFactory;
    private final SystemSettingsService systemSettingsService;
    private final String defaultKnowledgeBase;
    private final boolean pineconeEnabled;
    private final int maxResults;
    private final double minScore;

    @Autowired
    public PineconeKnowledgeRetrievalServiceImpl(
            @Qualifier("ibotEmbeddingModel") EmbeddingModel embeddingModel,
            PineconeEmbeddingStoreFactory pineconeEmbeddingStoreFactory,
            SystemSettingsService systemSettingsService,
            @Value("${ibot.rag.default-knowledge-base:default}") String defaultKnowledgeBase,
            @Value("${ibot.rag.pinecone.enabled:false}") boolean pineconeEnabled,
            @Value("${ibot.rag.retrieval.max-results:3}") int maxResults,
            @Value("${ibot.rag.retrieval.min-score:0.6}") double minScore) {
        this.embeddingModel = embeddingModel;
        this.pineconeEmbeddingStoreFactory = pineconeEmbeddingStoreFactory;
        this.systemSettingsService = systemSettingsService;
        this.defaultKnowledgeBase = defaultKnowledgeBase;
        this.pineconeEnabled = pineconeEnabled;
        this.maxResults = maxResults;
        this.minScore = minScore;
    }

    public PineconeKnowledgeRetrievalServiceImpl(EmbeddingModel embeddingModel,
                                                 PineconeEmbeddingStoreFactory pineconeEmbeddingStoreFactory,
                                                 String defaultKnowledgeBase,
                                                 boolean pineconeEnabled,
                                                 int maxResults,
                                                 double minScore) {
        this(embeddingModel, pineconeEmbeddingStoreFactory, new NoopSystemSettingsService(),
                defaultKnowledgeBase, pineconeEnabled, maxResults, minScore);
    }

    @Override
    public KnowledgeRetrievalResult retrieve(String knowledgeBase, String question) {
        String resolvedKnowledgeBase = StringUtils.hasText(knowledgeBase)
                ? knowledgeBase.trim()
                : systemSettingsService.getDefaultKnowledgeBase(defaultKnowledgeBase);
        String namespace = normalizeNamespace(resolvedKnowledgeBase);

        if (!pineconeEnabled || !StringUtils.hasText(question)) {
            return emptyResult(resolvedKnowledgeBase, namespace);
        }

        EmbeddingStore<TextSegment> embeddingStore = pineconeEmbeddingStoreFactory.create(namespace);
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(embeddingModel.embed(question).content())
                .maxResults(systemSettingsService.getRetrievalMaxResults(maxResults))
                .minScore(systemSettingsService.getRetrievalMinScore(minScore))
                .build();

        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(request);
        List<RetrievedSegment> segments = searchResult.matches().stream()
                .map(this::toRetrievedSegment)
                .toList();

        KnowledgeRetrievalResult result = new KnowledgeRetrievalResult();
        result.setKnowledgeBase(resolvedKnowledgeBase);
        result.setNamespace(namespace);
        result.setSegments(segments);
        result.setPromptContext(buildPromptContext(segments));
        return result;
    }

    private RetrievedSegment toRetrievedSegment(EmbeddingMatch<TextSegment> match) {
        Metadata metadata = match.embedded().metadata();
        return new RetrievedSegment(
                match.embedded().text(),
                match.score(),
                metadata.getString("source_path"),
                metadata.getString("file_name"),
                metadata.getString("chunk_index"));
    }

    private String buildPromptContext(List<RetrievedSegment> segments) {
        if (segments.isEmpty()) {
            return "No enterprise knowledge snippets were retrieved.";
        }

        return segments.stream()
                .map(segment -> "[score=" + String.format(Locale.US, "%.3f", segment.score())
                        + ", file=" + nullSafe(segment.fileName())
                        + ", chunk=" + nullSafe(segment.chunkIndex()) + "] "
                        + segment.content())
                .collect(Collectors.joining("\n\n"));
    }

    private KnowledgeRetrievalResult emptyResult(String knowledgeBase, String namespace) {
        return new KnowledgeRetrievalResult(
                knowledgeBase,
                namespace,
                "No enterprise knowledge snippets were retrieved.",
                List.of());
    }

    private String normalizeNamespace(String knowledgeBase) {
        return knowledgeBase.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-_]", "-");
    }

    private String nullSafe(String value) {
        return value == null ? "unknown" : value;
    }

    private static class NoopSystemSettingsService implements SystemSettingsService {

        @Override
        public void ensureDefaults() {
        }

        @Override
        public com.bbu.ibot.model.dto.AdminSettingsResponse getSettings() {
            return null;
        }

        @Override
        public com.bbu.ibot.model.dto.AdminSettingsResponse updateSettings(com.bbu.ibot.model.dto.UpdateAdminSettingsRequest request) {
            return null;
        }

        @Override
        public String getDefaultKnowledgeBase(String fallbackValue) {
            return fallbackValue;
        }

        @Override
        public int getRetrievalMaxResults(int fallbackValue) {
            return fallbackValue;
        }

        @Override
        public double getRetrievalMinScore(double fallbackValue) {
            return fallbackValue;
        }
    }
}
