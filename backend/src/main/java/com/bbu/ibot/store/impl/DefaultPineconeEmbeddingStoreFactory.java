package com.bbu.ibot.store.impl;

import com.bbu.ibot.store.PineconeEmbeddingStoreFactory;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Component
public class DefaultPineconeEmbeddingStoreFactory implements PineconeEmbeddingStoreFactory {

    private final String apiKey;
    private final String indexName;
    private final String metadataTextKey;

    public DefaultPineconeEmbeddingStoreFactory(
            @Value("${ibot.rag.pinecone.api-key:}") String apiKey,
            @Value("${ibot.rag.pinecone.index-name:}") String indexName,
            @Value("${ibot.rag.pinecone.metadata-text-key:text_segment}") String metadataTextKey) {
        this.apiKey = apiKey;
        this.indexName = indexName;
        this.metadataTextKey = metadataTextKey;
    }

    @Override
    public EmbeddingStore<TextSegment> create(String namespace) {
        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(indexName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "pinecone api-key or index-name is missing");
        }

        return new PineconeEmbeddingStore(apiKey, indexName, namespace, metadataTextKey, null, null, null);
    }
}
