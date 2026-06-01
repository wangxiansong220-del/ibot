package com.bbu.ibot.security;

import com.bbu.ibot.model.entity.UserAccount;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class AuthenticatedUser implements UserDetails {

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final String displayName;
    private final String role;
    private final boolean enabled;

    public AuthenticatedUser(UserAccount userAccount) {
        this.id = userAccount.getId();
        this.email = userAccount.getEmail();
        this.passwordHash = userAccount.getPasswordHash();
        this.displayName = userAccount.getDisplayName();
        this.role = userAccount.getRole();
        this.enabled = Boolean.TRUE.equals(userAccount.getEnabled());
    }

    @Override
    public Collection<SimpleGrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
