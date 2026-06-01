package com.bbu.ibot.service;

import com.bbu.ibot.agent.IbotAgent;
import com.bbu.ibot.model.dto.ChatCitationResponse;
import com.bbu.ibot.model.dto.ChatHistoryResponse;
import com.bbu.ibot.model.dto.ChatResponse;
import com.bbu.ibot.model.entity.ChatMemoryDocument;
import com.bbu.ibot.model.entity.ChatMessageRecord;
import com.bbu.ibot.model.vo.KnowledgeRetrievalResult;
import com.bbu.ibot.model.vo.RetrievedSegment;
import com.bbu.ibot.repository.ChatMemoryRepository;
import com.bbu.ibot.service.impl.IbotChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class IbotChatServiceImplTest {

    private ChatService chatService;
    private ChatMemoryRepository chatMemoryRepository;
    private IbotAgent ibotAgent;
    private KnowledgeRetrievalService knowledgeRetrievalService;

    @BeforeEach
    void setUp() {
        this.ibotAgent = mock(IbotAgent.class);
        this.chatMemoryRepository = mock(ChatMemoryRepository.class);
        this.knowledgeRetrievalService = mock(KnowledgeRetrievalService.class);
        given(knowledgeRetrievalService.retrieve(anyString(), anyString()))
                .willReturn(new KnowledgeRetrievalResult("default", "default",
                        "No enterprise knowledge snippets were retrieved.", List.of()));
        given(ibotAgent.chat(anyString(), anyString(), anyString())).willReturn("ok");
        this.chatService = new IbotChatServiceImpl(ibotAgent, chatMemoryRepository, knowledgeRetrievalService);
    }

    @Test
    void shouldAugmentChatWithRetrievedContext() {
        given(knowledgeRetrievalService.retrieve(eq("enterprise"), eq("what is rag")))
                .willReturn(new KnowledgeRetrievalResult(
                        "enterprise",
                        "enterprise",
                        "[score=0.990, file=handbook.txt, chunk=0] RAG uses retrieval before generation.",
                        List.of(new RetrievedSegment(
                                "RAG uses retrieval before generation.",
                                0.99,
                                "C:/kb/handbook.txt",
                                "handbook.txt",
                                "0"))));
        given(ibotAgent.chat(eq("session-1"), eq("what is rag"),
                eq("[score=0.990, file=handbook.txt, chunk=0] RAG uses retrieval before generation.")))
                .willReturn("rag answer");

        ChatResponse response = chatService.chat("session-1", "what is rag", "enterprise");

        assertEquals("rag answer", response.getAnswer());
        assertEquals(1, response.getCitations().size());
        ChatCitationResponse citation = response.getCitations().get(0);
        assertEquals("handbook.txt", citation.getFileName());
        assertEquals("0", citation.getChunkIndex());
    }

    @Test
    void shouldNotReturnCitationsForSimpleGreeting() {
        given(ibotAgent.chat(eq("session-1"), eq("你好"),
                eq("No enterprise knowledge snippets were retrieved.")))
                .willReturn("你好！有什么可以帮助你的吗？");

        ChatResponse response = chatService.chat("session-1", "你好", "enterprise");

        assertEquals("你好！有什么可以帮助你的吗？", response.getAnswer());
        assertEquals(0, response.getCitations().size());
        verify(knowledgeRetrievalService, never()).retrieve(anyString(), anyString());
    }

    @Test
    void shouldConvertSingleQuestionAnswerPair() {
        ChatMemoryDocument document = buildDocument(List.of(
                new ChatMessageRecord("USER", "what is rag"),
                new ChatMessageRecord("AI", "rag is retrieval augmented generation")
        ));
        given(chatMemoryRepository.findBySessionId("session-1")).willReturn(Optional.of(document));

        ChatHistoryResponse response = chatService.getHistory("session-1");

        assertEquals(1, response.getHistory().size());
        assertEquals("what is rag", response.getHistory().get(0).getQuestion());
        assertEquals("rag is retrieval augmented generation", response.getHistory().get(0).getAnswer());
    }

    @Test
    void shouldConvertMultipleQuestionAnswerPairsInOrder() {
        ChatMemoryDocument document = buildDocument(List.of(
                new ChatMessageRecord("USER", "question-1"),
                new ChatMessageRecord("AI", "answer-1"),
                new ChatMessageRecord("USER", "question-2"),
                new ChatMessageRecord("AI", "answer-2")
        ));
        given(chatMemoryRepository.findBySessionId("session-1")).willReturn(Optional.of(document));

        ChatHistoryResponse response = chatService.getHistory("session-1");

        assertEquals(2, response.getHistory().size());
        assertEquals("question-1", response.getHistory().get(0).getQuestion());
        assertEquals("answer-1", response.getHistory().get(0).getAnswer());
        assertEquals("question-2", response.getHistory().get(1).getQuestion());
        assertEquals("answer-2", response.getHistory().get(1).getAnswer());
    }

    @Test
    void shouldKeepLastQuestionWithEmptyAnswerWhenAiReplyMissing() {
        ChatMemoryDocument document = buildDocument(List.of(
                new ChatMessageRecord("USER", "question-1"),
                new ChatMessageRecord("AI", "answer-1"),
                new ChatMessageRecord("USER", "question-2")
        ));
        given(chatMemoryRepository.findBySessionId("session-1")).willReturn(Optional.of(document));

        ChatHistoryResponse response = chatService.getHistory("session-1");

        assertEquals(2, response.getHistory().size());
        assertEquals("question-2", response.getHistory().get(1).getQuestion());
        assertEquals("", response.getHistory().get(1).getAnswer());
    }

    @Test
    void shouldIgnoreSystemMessagesInHistoryResponse() {
        ChatMemoryDocument document = buildDocument(List.of(
                new ChatMessageRecord("SYSTEM", "system prompt"),
                new ChatMessageRecord("USER", "question-1"),
                new ChatMessageRecord("AI", "answer-1")
        ));
        given(chatMemoryRepository.findBySessionId("session-1")).willReturn(Optional.of(document));

        ChatHistoryResponse response = chatService.getHistory("session-1");

        assertEquals(1, response.getHistory().size());
        assertEquals("question-1", response.getHistory().get(0).getQuestion());
    }

    @Test
    void shouldReturnNotFoundWhenSessionDoesNotExist() {
        given(chatMemoryRepository.findBySessionId("missing")).willReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> chatService.getHistory("missing"));
    }

    private ChatMemoryDocument buildDocument(List<ChatMessageRecord> messages) {
        ChatMemoryDocument document = new ChatMemoryDocument();
        document.setSessionId("session-1");
        document.setCreatedAt(LocalDateTime.of(2026, 3, 30, 10, 0));
        document.setUpdatedAt(LocalDateTime.of(2026, 3, 30, 10, 0));
        document.setMessages(messages);
        return document;
    }
}
