package com.bbu.ibot.service;

import com.bbu.ibot.mapper.impl.MongoChatMemoryMapper;
import com.bbu.ibot.model.entity.ChatMemoryDocument;
import com.bbu.ibot.model.entity.ChatMessageRecord;
import com.bbu.ibot.repository.ChatMemoryRepository;
import com.bbu.ibot.service.impl.ChatMemoryServiceImpl;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ChatMemoryServiceImplTest {

    private ChatMemoryService chatMemoryService;
    private ChatMemoryRepository chatMemoryRepository;

    @BeforeEach
    void setUp() {
        this.chatMemoryRepository = mock(ChatMemoryRepository.class);
        this.chatMemoryService = new ChatMemoryServiceImpl(new MongoChatMemoryMapper(chatMemoryRepository));
    }

    @Test
    void shouldSaveAndLoadMessagesBySessionId() {
        chatMemoryService.saveMessages("session-a", List.of(
                UserMessage.userMessage("hello"),
                UserMessage.userMessage("please help me")
        ));

        ArgumentCaptor<ChatMemoryDocument> captor = ArgumentCaptor.forClass(ChatMemoryDocument.class);
        verify(chatMemoryRepository).save(captor.capture());
        ChatMemoryDocument saved = captor.getValue();
        given(chatMemoryRepository.findBySessionId("session-a")).willReturn(Optional.of(saved));

        List<ChatMessage> loaded = chatMemoryService.getMessages("session-a");
        assertEquals(2, loaded.size());
        assertEquals("hello", ((UserMessage) loaded.get(0)).singleText());
        assertEquals("please help me", ((UserMessage) loaded.get(1)).singleText());
    }

    @Test
    void shouldKeepSessionsIsolated() {
        ChatMemoryDocument sessionADocument = new ChatMemoryDocument();
        sessionADocument.setSessionId("session-a");
        sessionADocument.setMessages(List.of(new ChatMessageRecord("USER", "A")));
        ChatMemoryDocument sessionBDocument = new ChatMemoryDocument();
        sessionBDocument.setSessionId("session-b");
        sessionBDocument.setMessages(List.of(new ChatMessageRecord("USER", "B")));

        given(chatMemoryRepository.findBySessionId("session-a")).willReturn(Optional.of(sessionADocument));
        given(chatMemoryRepository.findBySessionId("session-b")).willReturn(Optional.of(sessionBDocument));

        List<ChatMessage> sessionA = chatMemoryService.getMessages("session-a");
        List<ChatMessage> sessionB = chatMemoryService.getMessages("session-b");

        assertEquals("A", ((UserMessage) sessionA.get(0)).singleText());
        assertEquals("B", ((UserMessage) sessionB.get(0)).singleText());
    }

    @Test
    void shouldIgnoreSystemMessageWhenLoadingChatMessages() {
        ChatMemoryDocument document = new ChatMemoryDocument();
        document.setSessionId("session-a");
        document.setMessages(List.of(
                new ChatMessageRecord("SYSTEM", "system prompt"),
                new ChatMessageRecord("USER", "user question")
        ));

        given(chatMemoryRepository.findBySessionId("session-a")).willReturn(Optional.of(document));

        List<ChatMessage> messages = chatMemoryService.getMessages("session-a");
        assertEquals(2, messages.size());
        assertEquals(SystemMessage.class, messages.get(0).getClass());
        assertEquals("user question", ((UserMessage) messages.get(1)).singleText());
    }

    @Test
    void shouldClearSessionMessages() {
        chatMemoryService.clearMessages("session-a");

        verify(chatMemoryRepository).deleteBySessionId("session-a");
        given(chatMemoryRepository.findBySessionId("session-a")).willReturn(Optional.empty());
        assertTrue(chatMemoryService.getMessages("session-a").isEmpty());
        verify(chatMemoryRepository, never()).save(any());
    }
}
