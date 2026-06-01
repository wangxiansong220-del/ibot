package com.bbu.ibot.service;

import com.bbu.ibot.model.dto.ChatHistoryResponse;
import com.bbu.ibot.model.dto.ChatResponse;

public interface ChatService {

    default ChatResponse chat(String sessionId, String message) {
        return chat(sessionId, message, null);
    }

    ChatResponse chat(String sessionId, String message, String knowledgeBase);

    ChatHistoryResponse getHistory(String sessionId);
}
