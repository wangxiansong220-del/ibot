package com.bbu.ibot.model.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class KnowledgeBaseRebuildResponse {

    private String taskId;
    private String knowledgeBase;
    private String namespace;
    private String status;
    private int progress;
    private int documentCount;
    private int processedDocuments;
    private int chunkCount;
    private String indexingStatus;
    private String message;
    private LocalDateTime triggeredAt;
    private LocalDateTime completedAt;
    private List<KnowledgeBaseRebuildDocumentResponse> documents = new ArrayList<>();

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getKnowledgeBase() {
        return knowledgeBase;
    }

    public void setKnowledgeBase(String knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getDocumentCount() {
        return documentCount;
    }

    public void setDocumentCount(int documentCount) {
        this.documentCount = documentCount;
    }

    public int getProcessedDocuments() {
        return processedDocuments;
    }

    public void setProcessedDocuments(int processedDocuments) {
        this.processedDocuments = processedDocuments;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(int chunkCount) {
        this.chunkCount = chunkCount;
    }

    public String getIndexingStatus() {
        return indexingStatus;
    }

    public void setIndexingStatus(String indexingStatus) {
        this.indexingStatus = indexingStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTriggeredAt() {
        return triggeredAt;
    }

    public void setTriggeredAt(LocalDateTime triggeredAt) {
        this.triggeredAt = triggeredAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public List<KnowledgeBaseRebuildDocumentResponse> getDocuments() {
        return documents;
    }

    public void setDocuments(List<KnowledgeBaseRebuildDocumentResponse> documents) {
        this.documents = documents;
    }
}
