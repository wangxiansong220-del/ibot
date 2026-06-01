package com.bbu.ibot.controller;

import com.bbu.ibot.model.dto.ChatHistoryResponse;
import com.bbu.ibot.model.dto.ChatRequest;
import com.bbu.ibot.model.dto.ChatResponse;
import com.bbu.ibot.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IbotChatController {

    private final ChatService chatService;

    public IbotChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/chat")
    public ChatResponse debugChat(@RequestParam(defaultValue = "debug-session") String sessionId,
                                  @RequestParam(defaultValue = "你好美") String q,
                                  @RequestParam(required = false) String knowledgeBase) {
        return chatService.chat(sessionId, q, knowledgeBase);
    }

    @PostMapping("/api/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return chatService.chat(request.getSessionId(), request.getMessage(), request.getKnowledgeBase());
    }

    @GetMapping("/api/chat/history")
    public ChatHistoryResponse history(@RequestParam String sessionId) {
        return chatService.getHistory(sessionId);
    }
}
