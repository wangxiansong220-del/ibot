package com.bbu.ibot.service.impl;

import com.bbu.ibot.mapper.ChatMemoryMapper;
import com.bbu.ibot.model.entity.ChatMemoryDocument;
import com.bbu.ibot.model.entity.ChatMessageRecord;
import com.bbu.ibot.service.ChatMemoryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChatMemoryServiceImpl implements ChatMemoryService {

    private final ChatMemoryMapper chatMemoryMapper;

    public ChatMemoryServiceImpl(ChatMemoryMapper chatMemoryMapper) {
        this.chatMemoryMapper = chatMemoryMapper;
    }

    @Override
    public List<ChatMessage> getMessages(String sessionId) {
        return chatMemoryMapper.findBySessionId(sessionId)
                .map(this::toChatMessages)
                .orElseGet(ArrayList::new);
    }

    @Override
    public void saveMessages(String sessionId, List<ChatMessage> messages) {
        ChatMemoryDocument document = chatMemoryMapper.findBySessionId(sessionId)
                .orElseGet(ChatMemoryDocument::new);
        document.setSessionId(sessionId);
        document.setMessages(toMessageRecords(messages));
        if (document.getCreatedAt() == null) {
            document.setCreatedAt(LocalDateTime.now());
        }
        document.setUpdatedAt(LocalDateTime.now());
        chatMemoryMapper.save(document);
    }

    @Override
    public void clearMessages(String sessionId) {
        chatMemoryMapper.deleteBySessionId(sessionId);
    }

    private List<ChatMessage> toChatMessages(ChatMemoryDocument document) {
        List<ChatMessage> messages = new ArrayList<>();
        for (ChatMessageRecord record : document.getMessages()) {
            messages.add(toChatMessage(record));
        }
        return messages;
    }

    private List<ChatMessageRecord> toMessageRecords(List<ChatMessage> messages) {
        List<ChatMessageRecord> records = new ArrayList<>();
        for (ChatMessage message : messages) {
            records.add(new ChatMessageRecord(message.type().name(), message.toString()));
            if (message instanceof UserMessage userMessage) {
                records.set(records.size() - 1, new ChatMessageRecord(message.type().name(), userMessage.singleText()));
            } else if (message instanceof AiMessage aiMessage) {
                records.set(records.size() - 1, new ChatMessageRecord(message.type().name(), aiMessage.text()));
            } else if (message instanceof SystemMessage systemMessage) {
                records.set(records.size() - 1, new ChatMessageRecord(message.type().name(), systemMessage.text()));
            }
        }
        return records;
    }

    private ChatMessage toChatMessage(ChatMessageRecord record) {
        return switch (record.getRole()) {
            case "USER" -> UserMessage.userMessage(record.getText());
            case "SYSTEM" -> SystemMessage.systemMessage(record.getText());
            case "AI" -> AiMessage.aiMessage(record.getText());
            default -> UserMessage.userMessage(record.getText());
        };
    }
}
