package com.uni.ms.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        long accessExpirationMs,
        long refreshExpirationMs,
        String issuer,
        String audience,
        long clockSkewSeconds
) {
}
