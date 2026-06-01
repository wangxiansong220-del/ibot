package com.bbu.ibot.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettingEntity {
    private String settingKey;
    private String settingValue;
    private String categoryName;
    private Boolean sensitive;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
