package com.uni.ms.identity.application;

import com.uni.ms.identity.domain.Role;
import com.uni.ms.identity.domain.User;
import com.uni.ms.identity.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class InitialAdminInitializer implements CommandLineRunner {

    private final InitialAdminProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (!properties.enabled()) {
            return;
        }
        validateConfiguration();
        String normalizedEmail = AuthenticationService.normalizeEmail(properties.email());
        if (userRepository.existsByEmail(normalizedEmail)) {
            return;
        }

        User user = new User();
        user.setName(properties.name().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(properties.password()));
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setRole(Role.ADMIN);
        userRepository.save(user);
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(properties.name())
                || !StringUtils.hasText(properties.email())
                || !StringUtils.hasText(properties.password())) {
            throw new IllegalStateException(
                    "Initial admin is enabled but required configuration is incomplete");
        }
    }
}
