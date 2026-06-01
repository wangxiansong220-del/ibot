package com.bbu.ibot.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @Email(message = "email format is invalid")
    @NotBlank(message = "email cannot be blank")
    private String email;

    @NotBlank(message = "password cannot be blank")
    private String password;
}
