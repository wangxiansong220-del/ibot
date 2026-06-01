package com.bbu.ibot.service.impl;

import com.bbu.ibot.mapper.SystemSettingMapper;
import com.bbu.ibot.model.dto.AdminSettingsResponse;
import com.bbu.ibot.model.dto.UpdateAdminSettingsRequest;
import com.bbu.ibot.model.entity.SystemSettingEntity;
import com.bbu.ibot.security.SecurityUtil;
import com.bbu.ibot.service.SystemSettingsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SystemSettingsServiceImpl implements SystemSettingsService {

    private static final String CATEGORY_RUNTIME = "runtime";

    private final SystemSettingMapper systemSettingMapper;
    private final String configuredChatModelName;
    private final String configuredDefaultKnowledgeBase;
    private final int configuredRetrievalMaxResults;
    private final double configuredRetrievalMinScore;
    private final int configuredUploadMaxSizeMb;
    private final boolean dashscopeApiConfigured;
    private final boolean pineconeApiConfigured;

    public SystemSettingsServiceImpl(SystemSettingMapper systemSettingMapper,
                                     @Value("${langchain4j.community.dashscope.chat-model.model-name:qwen-max}") String configuredChatModelName,
                                     @Value("${ibot.rag.default-knowledge-base:default}") String configuredDefaultKnowledgeBase,
                                     @Value("${ibot.rag.retrieval.max-results:3}") int configuredRetrievalMaxResults,
                                     @Value("${ibot.rag.retrieval.min-score:0.6}") double configuredRetrievalMinScore,
                                     @Value("${spring.servlet.multipart.max-file-size:20MB}") String configuredUploadMaxSize,
                                     @Value("${langchain4j.community.dashscope.chat-model.api-key:}") String dashscopeApiKey,
                                     @Value("${ibot.rag.pinecone.api-key:}") String pineconeApiKey) {
        this.systemSettingMapper = systemSettingMapper;
        this.configuredChatModelName = configuredChatModelName;
        this.configuredDefaultKnowledgeBase = configuredDefaultKnowledgeBase;
        this.configuredRetrievalMaxResults = configuredRetrievalMaxResults;
        this.configuredRetrievalMinScore = configuredRetrievalMinScore;
        this.configuredUploadMaxSizeMb = parseMaxSizeMb(configuredUploadMaxSize);
        this.dashscopeApiConfigured = StringUtils.hasText(dashscopeApiKey);
        this.pineconeApiConfigured = StringUtils.hasText(pineconeApiKey);
    }

    @Override
    public void ensureDefaults() {
        upsertDefault("chatModelName", configuredChatModelName);
        upsertDefault("defaultKnowledgeBase", configuredDefaultKnowledgeBase);
        upsertDefault("retrievalMaxResults", String.valueOf(configuredRetrievalMaxResults));
        upsertDefault("retrievalMinScore", String.valueOf(configuredRetrievalMinScore));
        upsertDefault("uploadMaxSizeMb", String.valueOf(configuredUploadMaxSizeMb));
    }

    @Override
    public AdminSettingsResponse getSettings() {
        Map<String, String> settings = toValueMap(systemSettingMapper.findAll());
        return AdminSettingsResponse.builder()
                .chatModelName(settings.getOrDefault("chatModelName", configuredChatModelName))
                .defaultKnowledgeBase(settings.getOrDefault("defaultKnowledgeBase", configuredDefaultKnowledgeBase))
                .retrievalMaxResults(Integer.parseInt(settings.getOrDefault("retrievalMaxResults", String.valueOf(configuredRetrievalMaxResults))))
                .retrievalMinScore(Double.parseDouble(settings.getOrDefault("retrievalMinScore", String.valueOf(configuredRetrievalMinScore))))
                .uploadMaxSizeMb(Integer.parseInt(settings.getOrDefault("uploadMaxSizeMb", String.valueOf(configuredUploadMaxSizeMb))))
                .dashscopeApiConfigured(dashscopeApiConfigured)
                .pineconeApiConfigured(pineconeApiConfigured)
                .build();
    }

    @Override
    public AdminSettingsResponse updateSettings(UpdateAdminSettingsRequest request) {
        save("chatModelName", request.getChatModelName());
        save("defaultKnowledgeBase", request.getDefaultKnowledgeBase());
        save("retrievalMaxResults", String.valueOf(request.getRetrievalMaxResults()));
        save("retrievalMinScore", String.valueOf(request.getRetrievalMinScore()));
        save("uploadMaxSizeMb", String.valueOf(request.getUploadMaxSizeMb()));
        return getSettings();
    }

    @Override
    public String getDefaultKnowledgeBase(String fallbackValue) {
        String value = valueOf("defaultKnowledgeBase");
        return StringUtils.hasText(value) ? value : fallbackValue;
    }

    @Override
    public int getRetrievalMaxResults(int fallbackValue) {
        String value = valueOf("retrievalMaxResults");
        return StringUtils.hasText(value) ? Integer.parseInt(value) : fallbackValue;
    }

    @Override
    public double getRetrievalMinScore(double fallbackValue) {
        String value = valueOf("retrievalMinScore");
        return StringUtils.hasText(value) ? Double.parseDouble(value) : fallbackValue;
    }

    private void upsertDefault(String key, String value) {
        if (systemSettingMapper.findByKey(key) == null) {
            save(key, value);
        }
    }

    private void save(String key, String value) {
        SystemSettingEntity entity = SystemSettingEntity.builder()
                .settingKey(key)
                .settingValue(value)
                .categoryName(CATEGORY_RUNTIME)
                .sensitive(Boolean.FALSE)
                .updatedBy(SecurityUtil.currentOperator())
                .updatedAt(LocalDateTime.now())
                .build();
        if (systemSettingMapper.findByKey(key) == null) {
            systemSettingMapper.insert(entity);
        } else {
            systemSettingMapper.update(entity);
        }
    }

    private String valueOf(String key) {
        SystemSettingEntity entity = systemSettingMapper.findByKey(key);
        return entity == null ? null : entity.getSettingValue();
    }

    private Map<String, String> toValueMap(List<SystemSettingEntity> entities) {
        Map<String, String> values = new HashMap<>();
        entities.forEach(entity -> values.put(entity.getSettingKey(), entity.getSettingValue()));
        return values;
    }

    private int parseMaxSizeMb(String configuredUploadMaxSize) {
        String normalized = configuredUploadMaxSize.trim().toUpperCase();
        if (normalized.endsWith("MB")) {
            return Integer.parseInt(normalized.replace("MB", "").trim());
        }
        return 20;
    }
}
