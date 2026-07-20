package com.uni.ms.identity.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.security.login-attempts")
public record LoginAttemptProperties(
        int maxFailures,
        Duration window
) {
}
