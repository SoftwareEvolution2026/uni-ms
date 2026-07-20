package com.uni.ms.identity.api;

import com.uni.ms.identity.domain.User;

public record UserSummary(
        Long id,
        String name,
        String email,
        String role
) {
    public static UserSummary from(User user) {
        return new UserSummary(user.getId(), user.getName(), user.getEmail(),
                user.getRole().name());
    }
}
