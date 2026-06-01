package com.bbu.ibot.model.dto;

import java.util.List;

public class SeedResponse {
    private String knowledgeBase;
    private int documentCount;
    private List<String> fileNames;
    private String message;

    public String getKnowledgeBase() { return knowledgeBase; }
    public void setKnowledgeBase(String knowledgeBase) { this.knowledgeBase = knowledgeBase; }
    public int getDocumentCount() { return documentCount; }
    public void setDocumentCount(int documentCount) { this.documentCount = documentCount; }
    public List<String> getFileNames() { return fileNames; }
    public void setFileNames(List<String> fileNames) { this.fileNames = fileNames; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
