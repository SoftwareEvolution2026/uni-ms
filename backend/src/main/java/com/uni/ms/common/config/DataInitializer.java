package com.uni.ms.common.config;

import com.uni.ms.user.Role;
import com.uni.ms.user.User;
import com.uni.ms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Seeds one demo account per role on first startup (idempotent). See README for credentials. */
@Component
@Profile({"local", "test"})
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seed("System Admin", "admin@uni.ms", "Admin123!", Role.ROLE_ADMIN);
        seed("Academic Manager", "manager@uni.ms", "Manager123!",
                Role.ROLE_ACADEMIC_MANAGER);
    }

    private void seed(String fullName, String email, String rawPassword, Role role) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.addRole(role);
        userRepository.save(user);
    }
}
