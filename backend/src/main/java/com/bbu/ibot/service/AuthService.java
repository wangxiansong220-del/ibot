package com.bbu.ibot.service;

import com.bbu.ibot.model.dto.AuthResponse;
import com.bbu.ibot.model.dto.LoginRequest;
import com.bbu.ibot.model.dto.RegisterRequest;
import com.bbu.ibot.model.dto.UserProfileResponse;
import com.bbu.ibot.security.AuthenticatedUser;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserProfileResponse currentUser(AuthenticatedUser authenticatedUser);
}
