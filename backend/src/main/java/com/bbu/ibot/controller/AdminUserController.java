package com.bbu.ibot.controller;

import com.bbu.ibot.model.dto.ApiMessageResponse;
import com.bbu.ibot.model.dto.CreateUserRequest;
import com.bbu.ibot.model.dto.ResetPasswordRequest;
import com.bbu.ibot.model.dto.UpdateUserRequest;
import com.bbu.ibot.model.dto.UserProfileResponse;
import com.bbu.ibot.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserAdminService userAdminService;

    public AdminUserController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public List<UserProfileResponse> listUsers() {
        return userAdminService.listUsers();
    }

    @PostMapping
    public UserProfileResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userAdminService.createUser(request);
    }

    @PatchMapping("/{userId}")
    public UserProfileResponse updateUser(@PathVariable Long userId,
                                          @Valid @RequestBody UpdateUserRequest request) {
        return userAdminService.updateUser(userId, request);
    }

    @PostMapping("/{userId}/reset-password")
    public ApiMessageResponse resetPassword(@PathVariable Long userId,
                                            @Valid @RequestBody ResetPasswordRequest request) {
        userAdminService.resetPassword(userId, request);
        return new ApiMessageResponse("password updated");
    }

    @DeleteMapping("/{userId}")
    public ApiMessageResponse deleteUser(@PathVariable Long userId) {
        userAdminService.deleteUser(userId);
        return new ApiMessageResponse("user deleted");
    }
}
