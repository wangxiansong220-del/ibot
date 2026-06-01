package com.bbu.ibot.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSettingsResponse {
    private String chatModelName;
    private String defaultKnowledgeBase;
    private Integer retrievalMaxResults;
    private Double retrievalMinScore;
    private Integer uploadMaxSizeMb;
    private Boolean dashscopeApiConfigured;
    private Boolean pineconeApiConfigured;
}
