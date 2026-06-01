package com.bbu.ibot.security;

import com.bbu.ibot.mapper.UserAccountMapper;
import com.bbu.ibot.model.entity.UserAccount;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final UserAccountMapper userAccountMapper;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, UserAccountMapper userAccountMapper) {
        this.jwtTokenService = jwtTokenService;
        this.userAccountMapper = userAccountMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtTokenService.parseToken(token);
                Long userId = Long.parseLong(claims.getSubject());
                UserAccount userAccount = userAccountMapper.findById(userId);
                if (userAccount != null && Boolean.TRUE.equals(userAccount.getEnabled())) {
                    AuthenticatedUser authenticatedUser = new AuthenticatedUser(userAccount);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(authenticatedUser, null, authenticatedUser.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
