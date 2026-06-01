package com.bbu.ibot.service;

import com.bbu.ibot.model.dto.CreateUserRequest;
import com.bbu.ibot.model.dto.ResetPasswordRequest;
import com.bbu.ibot.model.dto.UpdateUserRequest;
import com.bbu.ibot.model.dto.UserProfileResponse;

import java.util.List;

public interface UserAdminService {
    List<UserProfileResponse> listUsers();
    UserProfileResponse createUser(CreateUserRequest request);
    UserProfileResponse updateUser(Long userId, UpdateUserRequest request);
    void resetPassword(Long userId, ResetPasswordRequest request);
    void deleteUser(Long userId);
}
