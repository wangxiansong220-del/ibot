package com.bbu.ibot.store;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

public interface PineconeEmbeddingStoreFactory {

    EmbeddingStore<TextSegment> create(String namespace);
}
