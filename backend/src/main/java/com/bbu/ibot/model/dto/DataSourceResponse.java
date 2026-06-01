package com.bbu.ibot.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceResponse {
    private Long id;
    private String name;
    private String type;
    private String host;
    private Integer port;
    private String databaseName;
    private String username;
    private String apiBaseUrl;
    private String notes;
    private Boolean enabled;
    private Boolean hasPassword;
    private String lastTestStatus;
    private String lastTestMessage;
    private LocalDateTime lastTestedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
