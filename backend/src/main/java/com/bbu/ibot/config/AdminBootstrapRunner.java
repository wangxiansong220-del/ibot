package com.bbu.ibot.config;

import com.bbu.ibot.mapper.UserAccountMapper;
import com.bbu.ibot.model.entity.UserAccount;
import com.bbu.ibot.service.SystemSettingsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Locale;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final SystemSettingsService systemSettingsService;

    @Value("${ibot.auth.seed-admin.email}")
    private String seedAdminEmail;

    @Value("${ibot.auth.seed-admin.display-name}")
    private String seedAdminDisplayName;

    @Value("${ibot.auth.seed-admin.password}")
    private String seedAdminPassword;

    public AdminBootstrapRunner(UserAccountMapper userAccountMapper,
                                PasswordEncoder passwordEncoder,
                                SystemSettingsService systemSettingsService) {
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
        this.systemSettingsService = systemSettingsService;
    }

    @Override
    public void run(ApplicationArguments args) {
        systemSettingsService.ensureDefaults();
        if (userAccountMapper.countAdmins() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        UserAccount admin = UserAccount.builder()
                .email(seedAdminEmail.trim().toLowerCase(Locale.ROOT))
                .displayName(seedAdminDisplayName)
                .passwordHash(passwordEncoder.encode(seedAdminPassword))
                .role("ADMIN")
                .enabled(Boolean.TRUE)
                .createdAt(now)
                .updatedAt(now)
                .build();
        userAccountMapper.insert(admin);
    }
}
