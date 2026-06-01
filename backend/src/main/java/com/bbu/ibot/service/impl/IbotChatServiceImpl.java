package com.bbu.ibot.service.impl;

import com.bbu.ibot.agent.IbotAgent;
import com.bbu.ibot.model.dto.ChatCitationResponse;
import com.bbu.ibot.model.dto.ChatHistoryItemResponse;
import com.bbu.ibot.model.dto.ChatHistoryResponse;
import com.bbu.ibot.model.dto.ChatResponse;
import com.bbu.ibot.model.vo.KnowledgeRetrievalResult;
import com.bbu.ibot.model.entity.ChatMemoryDocument;
import com.bbu.ibot.model.entity.ChatMessageRecord;
import com.bbu.ibot.model.vo.RetrievedSegment;
import com.bbu.ibot.repository.ChatMemoryRepository;
import com.bbu.ibot.service.ChatService;
import com.bbu.ibot.service.KnowledgeRetrievalService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class IbotChatServiceImpl implements ChatService {

    private static final String EMPTY_KNOWLEDGE_PROMPT = "No enterprise knowledge snippets were retrieved.";
    private static final Set<String> SMALL_TALK_MESSAGES = Set.of(
            "hi", "hello", "hey", "你好", "您好", "哈喔", "嗨",
            "thanks", "thank you", "谢谢", "多谢",
            "bye", "goodbye", "再见", "拜拜");

    private final IbotAgent ibotAgent;
    private final ChatMemoryRepository chatMemoryRepository;
    private final KnowledgeRetrievalService knowledgeRetrievalService;

    public IbotChatServiceImpl(IbotAgent ibotAgent,
                               ChatMemoryRepository chatMemoryRepository,
                               KnowledgeRetrievalService knowledgeRetrievalService) {
        this.ibotAgent = ibotAgent;
        this.chatMemoryRepository = chatMemoryRepository;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
    }

    @Override
    public ChatResponse chat(String sessionId, String message, String knowledgeBase) {
        String normalizedSessionId = StringUtils.hasText(sessionId) ? sessionId.trim() : "default-session";
        String content = StringUtils.hasText(message) ? message.trim() : "hello";
        KnowledgeRetrievalResult retrievalResult = shouldUseKnowledgeRetrieval(content)
                ? knowledgeRetrievalService.retrieve(knowledgeBase, content)
                : emptyRetrievalResult(knowledgeBase);
        String answer = ibotAgent.chat(normalizedSessionId, content, retrievalResult.getPromptContext());
        return new ChatResponse(
                normalizedSessionId,
                answer,
                toCitations(retrievalResult.getSegments()));
    }

    @Override
    public ChatHistoryResponse getHistory(String sessionId) {
        String normalizedSessionId = StringUtils.hasText(sessionId) ? sessionId.trim() : null;
        if (!StringUtils.hasText(normalizedSessionId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionId cannot be blank");
        }

        ChatMemoryDocument document = chatMemoryRepository.findBySessionId(normalizedSessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "chat history not found"));

        ChatHistoryResponse response = new ChatHistoryResponse();
        response.setSessionId(document.getSessionId());
        response.setCreatedAt(document.getCreatedAt());
        response.setUpdatedAt(document.getUpdatedAt());
        response.setHistory(toHistoryItems(document.getMessages()));
        return response;
    }

    private List<ChatHistoryItemResponse> toHistoryItems(List<ChatMessageRecord> messages) {
        List<ChatHistoryItemResponse> items = new ArrayList<>();
        if (CollectionUtils.isEmpty(messages)) {
            return items;
        }

        ChatHistoryItemResponse current = null;

        for (ChatMessageRecord message : messages) {
            String role = message.getRole();
            if ("SYSTEM".equals(role)) {
                continue;
            }

            if ("USER".equals(role)) {
                if (current != null) {
                    items.add(current);
                }
                current = new ChatHistoryItemResponse(message.getText(), "");
                continue;
            }

            if ("AI".equals(role) && current != null && !StringUtils.hasText(current.getAnswer())) {
                current.setAnswer(message.getText());
            }
        }

        if (current != null) {
            items.add(current);
        }

        return items;
    }

    private List<ChatCitationResponse> toCitations(List<RetrievedSegment> segments) {
        List<ChatCitationResponse> citations = new ArrayList<>();
        if (CollectionUtils.isEmpty(segments)) {
            return citations;
        }

        for (RetrievedSegment segment : segments) {
            citations.add(new ChatCitationResponse(
                    segment.fileName(),
                    segment.sourcePath(),
                    segment.chunkIndex(),
                    segment.score(),
                    segment.content()));
        }
        return citations;
    }

    private boolean shouldUseKnowledgeRetrieval(String content) {
        if (!StringUtils.hasText(content)) {
            return false;
        }

        String normalized = content.trim().toLowerCase(Locale.ROOT);
        if (SMALL_TALK_MESSAGES.contains(normalized)) {
            return false;
        }

        if (normalized.length() <= 6 && (normalized.contains("你好") || normalized.contains("您好"))) {
            return false;
        }

        return true;
    }

    private KnowledgeRetrievalResult emptyRetrievalResult(String knowledgeBase) {
        String normalizedKnowledgeBase = StringUtils.hasText(knowledgeBase) ? knowledgeBase.trim() : "default";
        return new KnowledgeRetrievalResult(normalizedKnowledgeBase, normalizedKnowledgeBase, EMPTY_KNOWLEDGE_PROMPT, List.of());
    }
}
