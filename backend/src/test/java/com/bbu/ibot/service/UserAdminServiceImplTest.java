package com.bbu.ibot.service;

import com.bbu.ibot.mapper.UserAccountMapper;
import com.bbu.ibot.model.entity.UserAccount;
import com.bbu.ibot.service.impl.UserAdminServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceImplTest {

    @Mock
    private UserAccountMapper userAccountMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserAdminServiceImpl userAdminService;

    @Test
    void shouldDeleteNonAdminUser() {
        UserAccount user = UserAccount.builder()
                .id(2L)
                .email("user@ibot.local")
                .role("USER")
                .build();
        when(userAccountMapper.findById(2L)).thenReturn(user);

        userAdminService.deleteUser(2L);

        verify(userAccountMapper).deleteById(2L);
    }

    @Test
    void shouldRejectDeletingLastAdmin() {
        UserAccount admin = UserAccount.builder()
                .id(1L)
                .email("admin@ibot.local")
                .role("ADMIN")
                .build();
        when(userAccountMapper.findById(1L)).thenReturn(admin);
        when(userAccountMapper.countAdmins()).thenReturn(1);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> userAdminService.deleteUser(1L));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).isEqualTo("cannot delete the last admin account");
    }
}
