package com.bbu.ibot.config;

import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class IbotRagConfig {

    @Bean
    public EmbeddingModel ibotEmbeddingModel(
            @Value("${ibot.rag.embedding.api-key:}") String apiKey,
            @Value("${ibot.rag.embedding.model-name:text-embedding-v4}") String modelName,
            @Value("${ibot.rag.embedding.base-url:https://dashscope.aliyuncs.com/api/v1}") String baseUrl) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("Embedding api-key is missing");
        }
        return QwenEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .build();
    }
}
