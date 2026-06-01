package com.bbu.ibot.mapper;

import com.bbu.ibot.model.entity.ChatMemoryDocument;

import java.util.Optional;

public interface ChatMemoryMapper {

    Optional<ChatMemoryDocument> findBySessionId(String sessionId);

    ChatMemoryDocument save(ChatMemoryDocument document);

    void deleteBySessionId(String sessionId);
}
