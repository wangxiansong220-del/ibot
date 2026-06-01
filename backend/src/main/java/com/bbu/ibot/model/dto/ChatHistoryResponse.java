package com.bbu.ibot.model.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChatHistoryResponse {

    private String sessionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ChatHistoryItemResponse> history = new ArrayList<>();

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<ChatHistoryItemResponse> getHistory() {
        return history;
    }

    public void setHistory(List<ChatHistoryItemResponse> history) {
        this.history = history;
    }
}
