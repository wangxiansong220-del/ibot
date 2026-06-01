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
public class AdminTaskRecord {
    private Long id;
    private String taskType;
    private String targetType;
    private String targetName;
    private String status;
    private Integer progress;
    private String message;
    private String detailsJson;
    private String createdBy;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;
}
