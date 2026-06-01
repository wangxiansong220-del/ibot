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
public class UserProfileResponse {
    private Long id;
    private String email;
    private String displayName;
    private String role;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
