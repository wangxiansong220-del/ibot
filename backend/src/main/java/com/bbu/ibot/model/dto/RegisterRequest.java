package com.bbu.ibot.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @Email(message = "email format is invalid")
    @NotBlank(message = "email cannot be blank")
    private String email;

    @NotBlank(message = "displayName cannot be blank")
    @Size(max = 64, message = "displayName is too long")
    private String displayName;

    @NotBlank(message = "password cannot be blank")
    @Size(min = 6, max = 64, message = "password length must be between 6 and 64")
    private String password;
}
