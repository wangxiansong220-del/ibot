package com.bbu.ibot.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DataSourceRequest {
    @NotBlank(message = "name cannot be blank")
    @Size(max = 64, message = "name is too long")
    private String name;

    @NotBlank(message = "type cannot be blank")
    @Pattern(regexp = "MYSQL|POSTGRESQL|MONGODB|ELASTICSEARCH|REDIS|API_ENDPOINT", message = "unsupported data source type")
    private String type;

    private String host;
    private Integer port;
    private String databaseName;
    private String username;
    private String password;
    private String apiBaseUrl;
    private String notes;
    private Boolean enabled = Boolean.TRUE;
}
