package com.bbu.ibot.model.dto;

public class DocumentPreviewResponse {
    private String documentId;
    private String fileName;
    private String content;
    private long fileSize;
    private String knowledgeBase;

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public String getKnowledgeBase() { return knowledgeBase; }
    public void setKnowledgeBase(String knowledgeBase) { this.knowledgeBase = knowledgeBase; }
}
