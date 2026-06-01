package com.bbu.ibot.model.dto;

import jakarta.validation.constraints.NotBlank;

public class ChatRequest {

    @NotBlank(message = "sessionId cannot be blank")
    private String sessionId;

    @NotBlank(message = "message cannot be blank")
    private String message;

    private String knowledgeBase;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getKnowledgeBase() {
        return knowledgeBase;
    }

    public void setKnowledgeBase(String knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }
}
