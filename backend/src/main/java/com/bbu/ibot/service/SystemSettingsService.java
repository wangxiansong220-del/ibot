package com.bbu.ibot.service;

import com.bbu.ibot.model.dto.AdminSettingsResponse;
import com.bbu.ibot.model.dto.UpdateAdminSettingsRequest;

public interface SystemSettingsService {
    void ensureDefaults();
    AdminSettingsResponse getSettings();
    AdminSettingsResponse updateSettings(UpdateAdminSettingsRequest request);
    String getDefaultKnowledgeBase(String fallbackValue);
    int getRetrievalMaxResults(int fallbackValue);
    double getRetrievalMinScore(double fallbackValue);
}
