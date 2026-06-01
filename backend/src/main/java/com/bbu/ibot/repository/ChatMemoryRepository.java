package com.bbu.ibot.repository;

import com.bbu.ibot.model.entity.ChatMemoryDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ChatMemoryRepository extends MongoRepository<ChatMemoryDocument, String> {

    Optional<ChatMemoryDocument> findBySessionId(String sessionId);

    void deleteBySessionId(String sessionId);
}
