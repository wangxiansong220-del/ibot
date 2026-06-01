package com.bbu.ibot.model.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateAdminSettingsRequest {
    @NotBlank(message = "chatModelName cannot be blank")
    private String chatModelName;

    @NotBlank(message = "defaultKnowledgeBase cannot be blank")
    private String defaultKnowledgeBase;

    @Min(value = 1, message = "retrievalMaxResults must be at least 1")
    @Max(value = 10, message = "retrievalMaxResults must be at most 10")
    private Integer retrievalMaxResults;

    @DecimalMin(value = "0.0", message = "retrievalMinScore must be between 0 and 1")
    @DecimalMax(value = "1.0", message = "retrievalMinScore must be between 0 and 1")
    private Double retrievalMinScore;

    @Min(value = 1, message = "uploadMaxSizeMb must be at least 1")
    @Max(value = 100, message = "uploadMaxSizeMb must be at most 100")
    private Integer uploadMaxSizeMb;
}
