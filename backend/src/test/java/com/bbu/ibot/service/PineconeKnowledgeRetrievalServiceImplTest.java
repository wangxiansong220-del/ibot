package com.bbu.ibot.service;

import com.bbu.ibot.model.vo.KnowledgeRetrievalResult;
import com.bbu.ibot.service.impl.PineconeKnowledgeRetrievalServiceImpl;
import com.bbu.ibot.store.PineconeEmbeddingStoreFactory;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PineconeKnowledgeRetrievalServiceImplTest {

    @Test
    void shouldBuildPromptContextFromRetrievedSegments() {
        EmbeddingModel embeddingModel = textSegments -> Response.from(List.of(Embedding.from(new float[]{1.0f, 2.0f, 3.0f})));
        PineconeEmbeddingStoreFactory factory = namespace -> new RetrievalStore();

        KnowledgeRetrievalService service = new PineconeKnowledgeRetrievalServiceImpl(
                embeddingModel, factory, "enterprise", true, 3, 0.6);

        KnowledgeRetrievalResult result = service.retrieve("enterprise", "what is rag");

        assertEquals("enterprise", result.getNamespace());
        assertEquals(1, result.getSegments().size());
        assertTrue(result.getPromptContext().contains("employee-handbook.txt"));
        assertTrue(result.getPromptContext().contains("RAG combines retrieval"));
    }

    @Test
    void shouldReturnEmptyContextWhenRetrievalDisabled() {
        EmbeddingModel embeddingModel = textSegments -> Response.from(List.of(Embedding.from(new float[]{1.0f})));
        PineconeEmbeddingStoreFactory factory = namespace -> new RetrievalStore();

        KnowledgeRetrievalService service = new PineconeKnowledgeRetrievalServiceImpl(
                embeddingModel, factory, "enterprise", false, 3, 0.6);

        KnowledgeRetrievalResult result = service.retrieve("enterprise", "what is rag");

        assertEquals(0, result.getSegments().size());
        assertEquals("No enterprise knowledge snippets were retrieved.", result.getPromptContext());
    }

    private static class RetrievalStore implements EmbeddingStore<TextSegment> {

        @Override
        public String add(Embedding embedding) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(String id, Embedding embedding) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String add(Embedding embedding, TextSegment embedded) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> addAll(List<Embedding> embeddings) {
            throw new UnsupportedOperationException();
        }

        @Override
        public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
            TextSegment segment = TextSegment.from(
                    "RAG combines retrieval with generation for grounded answers.",
                    new Metadata()
                            .put("file_name", "employee-handbook.txt")
                            .put("source_path", "C:/kb/employee-handbook.txt")
                            .put("chunk_index", "0"));
            EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.92, "chunk-0", null, segment);
            return new EmbeddingSearchResult<>(List.of(match));
        }
    }
}
