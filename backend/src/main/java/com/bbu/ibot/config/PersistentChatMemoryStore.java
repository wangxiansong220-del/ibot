package com.bbu.ibot.config;

import com.bbu.ibot.service.ChatMemoryService;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.util.List;

public class PersistentChatMemoryStore implements ChatMemoryStore {

    private final ChatMemoryService chatMemoryService;

    public PersistentChatMemoryStore(ChatMemoryService chatMemoryService) {
        this.chatMemoryService = chatMemoryService;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        return chatMemoryService.getMessages(String.valueOf(memoryId));
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        chatMemoryService.saveMessages(String.valueOf(memoryId), messages);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        chatMemoryService.clearMessages(String.valueOf(memoryId));
    }
}
