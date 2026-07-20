package com.uni.ms.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.properties = properties;
    }

    public String generateAccessToken(Long userId, Collection<String> authorities) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + properties.accessExpirationMs());
        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(userId.toString())
                .audience().add(properties.audience()).and()
                .id(UUID.randomUUID().toString())
                .claim("authorities", authorities)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public AccessTokenClaims parseAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(properties.issuer())
                .clockSkewSeconds(properties.clockSkewSeconds())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Set<String> audience = claims.getAudience();
        if (audience == null || !audience.contains(properties.audience())) {
            throw new IllegalArgumentException("Invalid token audience");
        }
        return new AccessTokenClaims(
                Long.valueOf(claims.getSubject()),
                stringList(claims.get("authorities")),
                claims.getId(),
                claims.getExpiration().toInstant()
        );
    }

    public long accessExpirationSeconds() {
        return properties.accessExpirationMs() / 1000;
    }

    public long refreshExpirationMs() {
        return properties.refreshExpirationMs();
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof Collection<?> collection)) {
            return List.of();
        }
        return collection.stream().map(String::valueOf).toList();
    }

    public record AccessTokenClaims(
            Long userId,
            List<String> authorities,
            String tokenId,
            java.time.Instant expiresAt
    ) {
    }
}
