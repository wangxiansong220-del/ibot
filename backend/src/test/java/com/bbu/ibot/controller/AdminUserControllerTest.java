package com.bbu.ibot.controller;

import com.bbu.ibot.model.dto.CreateUserRequest;
import com.bbu.ibot.model.dto.UpdateUserRequest;
import com.bbu.ibot.model.dto.UserProfileResponse;
import com.bbu.ibot.service.UserAdminService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private UserAdminService userAdminService;

    @InjectMocks
    private AdminUserController adminUserController;

    @Test
    void shouldListUsers() {
        UserProfileResponse user = new UserProfileResponse();
        user.setId(1L);
        user.setEmail("user@ibot.local");
        user.setDisplayName("User");
        user.setRole("USER");
        user.setEnabled(true);

        when(userAdminService.listUsers()).thenReturn(List.of(user));

        List<UserProfileResponse> users = adminUserController.listUsers();

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getEmail()).isEqualTo("user@ibot.local");
    }

    @Test
    void shouldCreateUser() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("user@ibot.local");
        request.setDisplayName("User");
        request.setPassword("Temp@123456");
        request.setRole("USER");

        UserProfileResponse response = new UserProfileResponse();
        response.setId(2L);
        response.setEmail("user@ibot.local");

        when(userAdminService.createUser(request)).thenReturn(response);

        UserProfileResponse created = adminUserController.createUser(request);

        assertThat(created.getId()).isEqualTo(2L);
    }

    @Test
    void shouldUpdateUser() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setEnabled(false);

        UserProfileResponse response = new UserProfileResponse();
        response.setId(3L);
        response.setEnabled(false);

        when(userAdminService.updateUser(3L, request)).thenReturn(response);

        UserProfileResponse updated = adminUserController.updateUser(3L, request);

        assertThat(updated.getEnabled()).isFalse();
    }

    @Test
    void shouldDeleteUser() {
        var response = adminUserController.deleteUser(4L);

        verify(userAdminService).deleteUser(4L);
        assertThat(response.message()).isEqualTo("user deleted");
    }
}
