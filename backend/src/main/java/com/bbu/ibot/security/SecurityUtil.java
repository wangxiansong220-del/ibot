package com.bbu.ibot.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtil {

    private SecurityUtil() {
    }

    public static AuthenticatedUser currentUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            return null;
        }
        return authenticatedUser;
    }

    public static String currentOperator() {
        AuthenticatedUser user = currentUserOrNull();
        return user == null ? "system" : user.getEmail();
    }
}
