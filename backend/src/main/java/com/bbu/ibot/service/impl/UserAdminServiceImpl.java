package com.bbu.ibot.service.impl;

import com.bbu.ibot.mapper.UserAccountMapper;
import com.bbu.ibot.model.dto.CreateUserRequest;
import com.bbu.ibot.model.dto.ResetPasswordRequest;
import com.bbu.ibot.model.dto.UpdateUserRequest;
import com.bbu.ibot.model.dto.UserProfileResponse;
import com.bbu.ibot.model.entity.UserAccount;
import com.bbu.ibot.service.UserAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class UserAdminServiceImpl implements UserAdminService {

    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;

    public UserAdminServiceImpl(UserAccountMapper userAccountMapper, PasswordEncoder passwordEncoder) {
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<UserProfileResponse> listUsers() {
        return userAccountMapper.findAll().stream().map(AuthServiceImpl::toResponse).toList();
    }

    @Override
    public UserProfileResponse createUser(CreateUserRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userAccountMapper.findByEmail(email) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "email already registered");
        }

        LocalDateTime now = LocalDateTime.now();
        UserAccount userAccount = UserAccount.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName().trim())
                .role(request.getRole())
                .enabled(request.getEnabled() == null ? Boolean.TRUE : request.getEnabled())
                .createdAt(now)
                .updatedAt(now)
                .build();
        userAccountMapper.insert(userAccount);
        return AuthServiceImpl.toResponse(userAccount);
    }

    @Override
    public UserProfileResponse updateUser(Long userId, UpdateUserRequest request) {
        UserAccount userAccount = requireUser(userId);
        if (StringUtils.hasText(request.getDisplayName())) {
            userAccount.setDisplayName(request.getDisplayName().trim());
        }
        if (StringUtils.hasText(request.getRole())) {
            userAccount.setRole(request.getRole());
        }
        if (request.getEnabled() != null) {
            userAccount.setEnabled(request.getEnabled());
        }
        userAccount.setUpdatedAt(LocalDateTime.now());
        userAccountMapper.updateProfile(userAccount);
        return AuthServiceImpl.toResponse(userAccountMapper.findById(userId));
    }

    @Override
    public void resetPassword(Long userId, ResetPasswordRequest request) {
        requireUser(userId);
        userAccountMapper.updatePassword(userId, passwordEncoder.encode(request.getNewPassword()), LocalDateTime.now());
    }

    @Override
    public void deleteUser(Long userId) {
        UserAccount userAccount = requireUser(userId);
        if ("ADMIN".equalsIgnoreCase(userAccount.getRole()) && userAccountMapper.countAdmins() <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot delete the last admin account");
        }
        userAccountMapper.deleteById(userId);
    }

    private UserAccount requireUser(Long userId) {
        UserAccount userAccount = userAccountMapper.findById(userId);
        if (userAccount == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found");
        }
        return userAccount;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
