package com.bbu.ibot.mapper.impl;

import com.bbu.ibot.mapper.ChatMemoryMapper;
import com.bbu.ibot.model.entity.ChatMemoryDocument;
import com.bbu.ibot.repository.ChatMemoryRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class MongoChatMemoryMapper implements ChatMemoryMapper {

    private final ChatMemoryRepository chatMemoryRepository;

    public MongoChatMemoryMapper(ChatMemoryRepository chatMemoryRepository) {
        this.chatMemoryRepository = chatMemoryRepository;
    }

    @Override
    public Optional<ChatMemoryDocument> findBySessionId(String sessionId) {
        return chatMemoryRepository.findBySessionId(sessionId);
    }

    @Override
    public ChatMemoryDocument save(ChatMemoryDocument document) {
        if (document.getCreatedAt() == null) {
            document.setCreatedAt(LocalDateTime.now());
        }
        document.setUpdatedAt(LocalDateTime.now());
        return chatMemoryRepository.save(document);
    }

    @Override
    public void deleteBySessionId(String sessionId) {
        chatMemoryRepository.deleteBySessionId(sessionId);
    }
}
