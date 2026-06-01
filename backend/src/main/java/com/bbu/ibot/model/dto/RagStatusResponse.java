package com.bbu.ibot.model.dto;

public class RagStatusResponse {

    private String knowledgeBase;
    private String storageRoot;
    private int documentCount;
    private boolean ragEnabled;
    private String retrievalStatus;
    private String embeddingProvider;
    private String vectorStore;
    private int chunkSize;
    private int chunkOverlap;

    public String getKnowledgeBase() {
        return knowledgeBase;
    }

    public void setKnowledgeBase(String knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    public String getStorageRoot() {
        return storageRoot;
    }

    public void setStorageRoot(String storageRoot) {
        this.storageRoot = storageRoot;
    }

    public int getDocumentCount() {
        return documentCount;
    }

    public void setDocumentCount(int documentCount) {
        this.documentCount = documentCount;
    }

    public boolean isRagEnabled() {
        return ragEnabled;
    }

    public void setRagEnabled(boolean ragEnabled) {
        this.ragEnabled = ragEnabled;
    }

    public String getRetrievalStatus() {
        return retrievalStatus;
    }

    public void setRetrievalStatus(String retrievalStatus) {
        this.retrievalStatus = retrievalStatus;
    }

    public String getEmbeddingProvider() {
        return embeddingProvider;
    }

    public void setEmbeddingProvider(String embeddingProvider) {
        this.embeddingProvider = embeddingProvider;
    }

    public String getVectorStore() {
        return vectorStore;
    }

    public void setVectorStore(String vectorStore) {
        this.vectorStore = vectorStore;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getChunkOverlap() {
        return chunkOverlap;
    }

    public void setChunkOverlap(int chunkOverlap) {
        this.chunkOverlap = chunkOverlap;
    }
}
