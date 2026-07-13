package com.uni.ms.user.dto;

import com.uni.ms.user.Role;
import com.uni.ms.user.User;

import java.util.Set;
import java.util.stream.Collectors;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        boolean enabled,
        Set<String> roles
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.isEnabled(),
                user.getRoles().stream().map(Role::name).collect(Collectors.toSet())
        );
    }
}
