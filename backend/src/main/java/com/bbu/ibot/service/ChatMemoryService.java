package com.bbu.ibot.service;

import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

public interface ChatMemoryService {

    List<ChatMessage> getMessages(String sessionId);

    void saveMessages(String sessionId, List<ChatMessage> messages);

    void clearMessages(String sessionId);
}
