package com.uni.ms.identity.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.initial-admin")
public record InitialAdminProperties(
        boolean enabled,
        String name,
        String email,
        String password
) {
}
