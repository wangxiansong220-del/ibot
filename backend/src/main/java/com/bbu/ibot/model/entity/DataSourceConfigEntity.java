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
public class DataSourceConfigEntity {
    private Long id;
    private String name;
    private String type;
    private String host;
    private Integer port;
    private String databaseName;
    private String username;
    private String secretValue;
    private String apiBaseUrl;
    private String notes;
    private Boolean enabled;
    private String lastTestStatus;
    private String lastTestMessage;
    private LocalDateTime lastTestedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
