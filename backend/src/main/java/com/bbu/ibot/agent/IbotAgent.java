package com.bbu.ibot.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(
        wiringMode = EXPLICIT,
        chatModel = "qwenChatModel",
        chatMemoryProvider = "ibotChatMemoryProvider",
        tools = {"ibotKnowledgeTools"}
)
public interface IbotAgent {

    @SystemMessage({
            "You are ibot, an enterprise knowledge assistant.",
            "Use the retrieved enterprise context below when it is relevant to the user's question.",
            "If the context is empty or not relevant, answer honestly and do not pretend the enterprise knowledge base contained the answer.",
            "Retrieved context:",
            "{{retrievedContext}}"
    })
    String chat(@MemoryId String sessionId,
                @UserMessage String message,
                @V("retrievedContext") String retrievedContext);
}
