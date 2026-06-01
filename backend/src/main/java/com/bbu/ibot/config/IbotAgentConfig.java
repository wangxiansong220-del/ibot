package com.bbu.ibot.config;

import com.bbu.ibot.service.ChatMemoryService;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class IbotAgentConfig {

    @Value("${ibot.chat.memory.max-messages:20}")
    private int maxMessages;

    @Bean
    public ChatMemoryStore ibotChatMemoryStore(ChatMemoryService chatMemoryService) {
        return new PersistentChatMemoryStore(chatMemoryService);
    }

    @Bean
    public ChatMemoryProvider ibotChatMemoryProvider(ChatMemoryStore ibotChatMemoryStore) {
        return memoryId -> buildChatMemory(memoryId, ibotChatMemoryStore);
    }

    private ChatMemory buildChatMemory(Object memoryId, ChatMemoryStore chatMemoryStore) {
        return MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(maxMessages)
                .chatMemoryStore(chatMemoryStore)
                .build();
    }
}
