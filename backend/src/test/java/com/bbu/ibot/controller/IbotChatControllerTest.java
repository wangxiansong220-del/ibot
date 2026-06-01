package com.bbu.ibot.controller;

import com.bbu.ibot.model.dto.ChatHistoryItemResponse;
import com.bbu.ibot.model.dto.ChatHistoryResponse;
import com.bbu.ibot.model.dto.ChatRequest;
import com.bbu.ibot.model.dto.ChatResponse;
import com.bbu.ibot.service.ChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IbotChatControllerTest {

    @Mock
    private ChatService chatService;

    @InjectMocks
    private IbotChatController ibotChatController;

    @Test
    void shouldReturnChatResponseForPostApiChat() {
        ChatRequest request = new ChatRequest();
        request.setSessionId("session-1");
        request.setMessage("hello");
        request.setKnowledgeBase("enterprise");

        ChatResponse response = new ChatResponse();
        response.setSessionId("session-1");
        response.setAnswer("hello, i am ibot");

        when(chatService.chat("session-1", "hello", "enterprise")).thenReturn(response);

        ChatResponse actual = ibotChatController.chat(request);

        assertThat(actual.getSessionId()).isEqualTo("session-1");
        assertThat(actual.getAnswer()).isEqualTo("hello, i am ibot");
    }

    @Test
    void shouldKeepDebugEndpointAvailable() {
        ChatResponse response = new ChatResponse();
        response.setSessionId("debug-session");
        response.setAnswer("debug ok");

        when(chatService.chat("debug-session", "hello", "enterprise")).thenReturn(response);

        ChatResponse actual = ibotChatController.debugChat("debug-session", "hello", "enterprise");

        assertThat(actual.getSessionId()).isEqualTo("debug-session");
        assertThat(actual.getAnswer()).isEqualTo("debug ok");
    }

    @Test
    void shouldReturnHistoryForExistingSession() {
        ChatHistoryResponse response = new ChatHistoryResponse();
        response.setSessionId("session-1");
        response.setCreatedAt(LocalDateTime.of(2026, 3, 30, 10, 0));
        response.setUpdatedAt(LocalDateTime.of(2026, 3, 30, 10, 5));
        response.setHistory(List.of(new ChatHistoryItemResponse("what is rag", "rag is retrieval augmented generation")));

        when(chatService.getHistory("session-1")).thenReturn(response);

        ChatHistoryResponse actual = ibotChatController.history("session-1");

        assertThat(actual.getHistory()).hasSize(1);
        assertThat(actual.getHistory().get(0).getQuestion()).isEqualTo("what is rag");
        assertThat(actual.getHistory().get(0).getAnswer()).isEqualTo("rag is retrieval augmented generation");
        verify(chatService).getHistory("session-1");
    }
}
