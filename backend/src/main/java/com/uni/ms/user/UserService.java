package com.uni.ms.user;

import com.uni.ms.common.audit.AuditService;
import com.uni.ms.common.exception.ApiException;
import com.uni.ms.common.exception.ResourceNotFoundException;
import com.uni.ms.user.dto.CreateUserRequest;
import com.uni.ms.user.dto.UpdateUserRequest;
import com.uni.ms.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public UserResponse createUser(CreateUserRequest request, String actorEmail) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoles(parseRoles(request.roles()));
        userRepository.save(user);

        auditService.record(actorEmail, "USER_CREATED", "Created " + request.email()
                + " with roles " + request.roles());
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request, String actorEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        boolean emailChanged = !user.getEmail().equalsIgnoreCase(request.email());
        if (emailChanged && userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }

        Set<Role> roles = parseRoles(request.roles());
        boolean editingSelf = user.getEmail().equalsIgnoreCase(actorEmail);
        if (editingSelf && (!roles.contains(Role.ROLE_ADMIN) || !request.enabled())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "You cannot remove your own admin role or disable your own account");
        }

        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setRoles(roles);
        user.setEnabled(request.enabled());
        userRepository.save(user);

        auditService.record(actorEmail, "USER_UPDATED", "Updated " + user.getEmail());
        return UserResponse.from(user);
    }

    @Transactional
    public void deleteUser(Long id, String actorEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        if (user.getEmail().equalsIgnoreCase(actorEmail)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot delete your own account");
        }
        userRepository.delete(user);
        auditService.record(actorEmail, "USER_DELETED", "Deleted " + user.getEmail());
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrent(String email) {
        return UserResponse.from(getByEmail(email));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listAll() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    private Set<Role> parseRoles(Set<String> roleNames) {
        Set<Role> roles = new HashSet<>();
        for (String name : roleNames) {
            try {
                roles.add(Role.valueOf(name));
            } catch (IllegalArgumentException e) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid role: " + name);
            }
        }
        return roles;
    }
}
