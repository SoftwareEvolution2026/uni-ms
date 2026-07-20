package com.uni.ms.identity.application;

import com.uni.ms.common.audit.AuditService;
import com.uni.ms.common.exception.ErrorCode;
import com.uni.ms.common.exception.ProblemException;
import com.uni.ms.common.security.JwtService;
import com.uni.ms.identity.domain.RefreshToken;
import com.uni.ms.identity.domain.User;
import com.uni.ms.identity.infrastructure.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final JwtService jwtService;
    private final AuditService auditService;

    @Transactional
    public IssuedRefreshToken issue(User user) {
        return issue(user, UUID.randomUUID());
    }

    @Transactional(noRollbackFor = ProblemException.class)
    public RotatedRefreshToken rotate(String rawToken) {
        RefreshToken current = repository.findByTokenHashForUpdate(hash(rawToken))
                .orElseThrow(this::invalidToken);
        Instant now = Instant.now();

        if (current.isRevoked()) {
            if (current.wasRotated()) {
                repository.revokeActiveFamily(current.getFamilyId());
                repository.revokeAllActiveForUser(current.getUser().getId());
                auditService.recordIndependently(current.getUser().getEmail(),
                        "REFRESH_TOKEN_REUSE", "Suspicious rotated-token reuse detected");
                throw new ProblemException(HttpStatus.UNAUTHORIZED,
                        ErrorCode.REFRESH_TOKEN_REUSED,
                        "Refresh token reuse was detected; active sessions were revoked");
            }
            throw invalidToken();
        }
        if (current.isExpired(now)) {
            current.revoke(now);
            throw invalidToken();
        }

        IssuedRefreshToken replacement = issue(current.getUser(), current.getFamilyId());
        current.revoke(now);
        current.setReplacedByTokenHash(hash(replacement.rawToken()));
        repository.save(current);
        auditService.record(current.getUser().getEmail(), "REFRESH_SUCCESS",
                "Refresh token rotated");
        return new RotatedRefreshToken(current.getUser(), replacement.rawToken());
    }

    @Transactional
    public void logout(String rawToken) {
        repository.findByTokenHashForUpdate(hash(rawToken)).ifPresent(token -> {
            if (!token.isRevoked()) {
                token.revoke(Instant.now());
                repository.save(token);
            }
            auditService.record(token.getUser().getEmail(), "LOGOUT", "Session logged out");
        });
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        repository.revokeAllActiveForUser(userId);
    }

    private IssuedRefreshToken issue(User user, UUID familyId) {
        String rawToken = generateOpaqueToken();
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hash(rawToken));
        token.setFamilyId(familyId);
        token.setExpiresAt(Instant.now().plusMillis(jwtService.refreshExpirationMs()));
        repository.save(token);
        return new IssuedRefreshToken(rawToken);
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private ProblemException invalidToken() {
        return new ProblemException(HttpStatus.UNAUTHORIZED,
                ErrorCode.REFRESH_TOKEN_INVALID, "The refresh token is invalid or expired");
    }

    public record IssuedRefreshToken(String rawToken) {
    }

    public record RotatedRefreshToken(User user, String rawToken) {
    }
}
