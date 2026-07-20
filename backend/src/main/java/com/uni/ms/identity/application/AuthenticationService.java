package com.uni.ms.identity.application;

import com.uni.ms.common.audit.AuditService;
import com.uni.ms.common.exception.ErrorCode;
import com.uni.ms.common.exception.ProblemException;
import com.uni.ms.common.security.JwtService;
import com.uni.ms.identity.api.AuthResponse;
import com.uni.ms.identity.api.ChangePasswordRequest;
import com.uni.ms.identity.api.LoginRequest;
import com.uni.ms.identity.api.UserSummary;
import com.uni.ms.identity.domain.User;
import com.uni.ms.identity.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private static final String DUMMY_BCRYPT_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptPolicy loginAttemptPolicy;
    private final AuditService auditService;

    @Transactional
    public AuthResponse login(LoginRequest request, String clientIp) {
        String email = normalizeEmail(request.email());
        if (loginAttemptPolicy.isBlocked(email, clientIp)) {
            auditService.recordIndependently(email, "LOGIN_RATE_LIMITED",
                    "Login blocked for client IP " + clientIp);
            throw new ProblemException(HttpStatus.TOO_MANY_REQUESTS,
                    ErrorCode.TOO_MANY_LOGIN_ATTEMPTS,
                    "Too many failed login attempts; try again later");
        }

        Optional<User> candidate = userRepository.findByEmail(email);
        String hash = candidate.map(User::getPasswordHash).orElse(DUMMY_BCRYPT_HASH);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
        if (candidate.isEmpty() || !passwordMatches) {
            loginAttemptPolicy.recordFailure(email, clientIp);
            auditService.recordIndependently(email, "LOGIN_FAILURE",
                    "Invalid login attempt from client IP " + clientIp);
            throw new ProblemException(HttpStatus.UNAUTHORIZED,
                    ErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
        }

        User user = candidate.orElseThrow();
        if (!user.isEnabled()) {
            loginAttemptPolicy.recordFailure(email, clientIp);
            auditService.recordIndependently(email, "LOGIN_FAILURE",
                    "Disabled-account login attempt from client IP " + clientIp);
            throw new ProblemException(HttpStatus.UNAUTHORIZED,
                    ErrorCode.USER_DISABLED, "The user account is disabled");
        }

        loginAttemptPolicy.reset(email, clientIp);
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issue(user);
        auditService.record(user.getEmail(), "LOGIN_SUCCESS", "Successful login");
        return response(user, refreshToken.rawToken());
    }

    @Transactional(noRollbackFor = ProblemException.class)
    public AuthResponse refresh(String rawToken) {
        RefreshTokenService.RotatedRefreshToken rotated = refreshTokenService.rotate(rawToken);
        return response(rotated.user(), rotated.rawToken());
    }

    @Transactional
    public void logout(String rawToken) {
        refreshTokenService.logout(rawToken);
    }

    @Transactional(readOnly = true)
    public UserSummary me(Long userId) {
        return UserSummary.from(requireUser(userId));
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = requireUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ProblemException(HttpStatus.BAD_REQUEST,
                    ErrorCode.INVALID_CREDENTIALS, "Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(userId);
        auditService.record(user.getEmail(), "PASSWORD_CHANGED",
                "Password changed; active refresh tokens revoked");
    }

    private AuthResponse response(User user, String refreshToken) {
        String accessToken = jwtService.generateAccessToken(user.getId(),
                List.of(user.getRole().authority()));
        return AuthResponse.bearer(accessToken, refreshToken,
                jwtService.accessExpirationSeconds(), UserSummary.from(user));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .filter(User::isEnabled)
                .orElseThrow(() -> new ProblemException(HttpStatus.UNAUTHORIZED,
                        ErrorCode.USER_DISABLED, "The user account is unavailable"));
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
