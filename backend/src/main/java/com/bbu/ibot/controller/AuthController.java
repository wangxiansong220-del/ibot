package com.bbu.ibot.controller;

import com.bbu.ibot.model.dto.AuthResponse;
import com.bbu.ibot.model.dto.LoginRequest;
import com.bbu.ibot.model.dto.RegisterRequest;
import com.bbu.ibot.model.dto.UserProfileResponse;
import com.bbu.ibot.security.AuthenticatedUser;
import com.bbu.ibot.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserProfileResponse me(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return authService.currentUser(authenticatedUser);
    }
}
