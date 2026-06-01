package com.bbu.ibot.model.dto;

import java.time.LocalDateTime;
import java.util.List;

public class KnowledgeDocumentResponse {

    private String documentId;
    private String knowledgeBase;
    private String namespace;
    private String fileName;
    private long fileSize;
    private int chunkCount;
    private String storagePath;
    private String indexingStatus;
    private LocalDateTime uploadedAt;
    private List<String> tags;
    private String folder;

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getKnowledgeBase() {
        return knowledgeBase;
    }

    public void setKnowledgeBase(String knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    public String getFileName() {
        return fileName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(int chunkCount) {
        this.chunkCount = chunkCount;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getIndexingStatus() {
        return indexingStatus;
    }

    public void setIndexingStatus(String indexingStatus) {
        this.indexingStatus = indexingStatus;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }
}
