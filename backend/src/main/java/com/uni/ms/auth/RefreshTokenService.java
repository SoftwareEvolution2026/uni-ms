package com.uni.ms.auth;

import com.uni.ms.common.exception.ApiException;
import com.uni.ms.common.security.JwtProperties;
import com.uni.ms.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/** Opaque refresh tokens: only the SHA-256 hash is stored; rotated on every use. */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final JwtProperties jwtProperties;

    @Transactional
    public String issue(User user) {
        String raw = UUID.randomUUID().toString() + UUID.randomUUID();
        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(hash(raw));
        entity.setExpiresAt(Instant.now().plusMillis(jwtProperties.refreshExpirationMs()));
        repository.save(entity);
        return raw;
    }

    @Transactional(readOnly = true)
    public User verify(String rawToken) {
        RefreshToken token = repository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,
                        "Invalid refresh token"));
        if (!token.isActive()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token expired or revoked");
        }
        return token.getUser();
    }

    /** Revoke the presented token and issue a fresh one. */
    @Transactional
    public String rotate(String oldRawToken, User user) {
        RefreshToken existing = repository.findByTokenHash(hash(oldRawToken))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,
                        "Invalid refresh token"));
        existing.setRevoked(true);
        repository.save(existing);
        return issue(user);
    }

    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(hash(rawToken)).ifPresent(t -> {
            t.setRevoked(true);
            repository.save(t);
        });
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] out = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
