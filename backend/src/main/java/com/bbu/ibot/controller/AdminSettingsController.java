package com.bbu.ibot.controller;

import com.bbu.ibot.model.dto.AdminSettingsResponse;
import com.bbu.ibot.model.dto.UpdateAdminSettingsRequest;
import com.bbu.ibot.service.SystemSettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/settings")
public class AdminSettingsController {

    private final SystemSettingsService systemSettingsService;

    public AdminSettingsController(SystemSettingsService systemSettingsService) {
        this.systemSettingsService = systemSettingsService;
    }

    @GetMapping
    public AdminSettingsResponse getSettings() {
        return systemSettingsService.getSettings();
    }

    @PutMapping
    public AdminSettingsResponse updateSettings(@Valid @RequestBody UpdateAdminSettingsRequest request) {
        return systemSettingsService.updateSettings(request);
    }
}
