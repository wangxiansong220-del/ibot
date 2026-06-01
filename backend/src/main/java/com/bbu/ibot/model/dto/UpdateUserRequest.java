package com.bbu.ibot.model.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {
    @Size(max = 64, message = "displayName is too long")
    private String displayName;

    @Pattern(regexp = "USER|ADMIN", message = "role must be USER or ADMIN")
    private String role;

    private Boolean enabled;
}
