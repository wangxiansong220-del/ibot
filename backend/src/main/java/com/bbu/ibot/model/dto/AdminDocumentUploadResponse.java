package com.bbu.ibot.model.dto;

import java.util.ArrayList;
import java.util.List;

public class AdminDocumentUploadResponse {

    private String knowledgeBase;
    private int uploadedCount;
    private String message;
    private List<KnowledgeDocumentResponse> documents = new ArrayList<>();

    public String getKnowledgeBase() {
        return knowledgeBase;
    }

    public void setKnowledgeBase(String knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    public int getUploadedCount() {
        return uploadedCount;
    }

    public void setUploadedCount(int uploadedCount) {
        this.uploadedCount = uploadedCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<KnowledgeDocumentResponse> getDocuments() {
        return documents;
    }

    public void setDocuments(List<KnowledgeDocumentResponse> documents) {
        this.documents = documents;
    }
}
