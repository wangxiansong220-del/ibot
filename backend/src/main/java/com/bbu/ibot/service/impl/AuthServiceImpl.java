package com.bbu.ibot.service.impl;

import com.bbu.ibot.mapper.UserAccountMapper;
import com.bbu.ibot.model.dto.AuthResponse;
import com.bbu.ibot.model.dto.LoginRequest;
import com.bbu.ibot.model.dto.RegisterRequest;
import com.bbu.ibot.model.dto.UserProfileResponse;
import com.bbu.ibot.model.entity.UserAccount;
import com.bbu.ibot.security.AuthenticatedUser;
import com.bbu.ibot.security.JwtTokenService;
import com.bbu.ibot.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthServiceImpl(UserAccountMapper userAccountMapper,
                           PasswordEncoder passwordEncoder,
                           JwtTokenService jwtTokenService) {
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userAccountMapper.findByEmail(email) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "email already registered");
        }

        LocalDateTime now = LocalDateTime.now();
        UserAccount userAccount = UserAccount.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName().trim())
                .role("USER")
                .enabled(Boolean.TRUE)
                .createdAt(now)
                .updatedAt(now)
                .build();
        userAccountMapper.insert(userAccount);

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(userAccount);
        return AuthResponse.builder()
                .token(jwtTokenService.generateToken(authenticatedUser))
                .user(toResponse(userAccount))
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        UserAccount userAccount = userAccountMapper.findByEmail(email);
        if (userAccount == null || !passwordEncoder.matches(request.getPassword(), userAccount.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "email or password is incorrect");
        }
        if (!Boolean.TRUE.equals(userAccount.getEnabled())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "account is disabled");
        }

        LocalDateTime now = LocalDateTime.now();
        userAccountMapper.updateLoginInfo(userAccount.getId(), now, now);
        userAccount.setLastLoginAt(now);
        userAccount.setUpdatedAt(now);

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(userAccount);
        return AuthResponse.builder()
                .token(jwtTokenService.generateToken(authenticatedUser))
                .user(toResponse(userAccount))
                .build();
    }

    @Override
    public UserProfileResponse currentUser(AuthenticatedUser authenticatedUser) {
        UserAccount userAccount = userAccountMapper.findById(authenticatedUser.getId());
        if (userAccount == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not found");
        }
        return toResponse(userAccount);
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email cannot be blank");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public static UserProfileResponse toResponse(UserAccount userAccount) {
        return UserProfileResponse.builder()
                .id(userAccount.getId())
                .email(userAccount.getEmail())
                .displayName(userAccount.getDisplayName())
                .role(userAccount.getRole())
                .enabled(userAccount.getEnabled())
                .createdAt(userAccount.getCreatedAt())
                .lastLoginAt(userAccount.getLastLoginAt())
                .build();
    }
}
