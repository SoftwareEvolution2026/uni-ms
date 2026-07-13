package com.uni.ms.auth;

import com.uni.ms.auth.dto.AuthResponse;
import com.uni.ms.auth.dto.LoginRequest;
import com.uni.ms.common.audit.AuditService;
import com.uni.ms.common.exception.ApiException;
import com.uni.ms.common.security.JwtService;
import com.uni.ms.user.Role;
import com.uni.ms.user.User;
import com.uni.ms.user.UserRepository;
import com.uni.ms.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        auditService.record(user.getEmail(), "USER_LOGIN", "Successful login");
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        User user = refreshTokenService.verify(refreshToken);
        String newRefresh = refreshTokenService.rotate(refreshToken, user);
        String accessToken = jwtService.generateAccessToken(user.getEmail(), roleNames(user));

        auditService.record(user.getEmail(), "TOKEN_REFRESH", "Access token refreshed");
        return AuthResponse.of(accessToken, newRefresh, UserResponse.from(user));
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getEmail(), roleNames(user));
        String refreshToken = refreshTokenService.issue(user);
        return AuthResponse.of(accessToken, refreshToken, UserResponse.from(user));
    }

    private List<String> roleNames(User user) {
        return user.getRoles().stream().map(Role::name).toList();
    }
}
