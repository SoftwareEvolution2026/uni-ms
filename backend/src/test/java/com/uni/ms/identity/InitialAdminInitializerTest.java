package com.uni.ms.identity;

import com.uni.ms.identity.application.InitialAdminInitializer;
import com.uni.ms.identity.application.InitialAdminProperties;
import com.uni.ms.identity.domain.Role;
import com.uni.ms.identity.domain.User;
import com.uni.ms.identity.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitialAdminInitializerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void productionDefaultDoesNotCreateDemoAccount() throws Exception {
        InitialAdminInitializer initializer = initializer(
                new InitialAdminProperties(false, "", "", ""));

        initializer.run();

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void enabledInitializationRequiresEveryValue() {
        InitialAdminInitializer initializer = initializer(
                new InitialAdminProperties(true, "Admin", "", "secret"));

        assertThrows(IllegalStateException.class, initializer::run);
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void existingAdminIsNeverRecreated() throws Exception {
        when(userRepository.existsByEmail("admin@university.test")).thenReturn(true);
        InitialAdminInitializer initializer = initializer(new InitialAdminProperties(
                true, "Admin", " ADMIN@UNIVERSITY.TEST ", "StrongPassword123!"));

        initializer.run();

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void createsNormalizedVerifiedAdminWithoutExposingPassword() throws Exception {
        when(passwordEncoder.encode("StrongPassword123!")).thenReturn("bcrypt-hash");
        InitialAdminInitializer initializer = initializer(new InitialAdminProperties(
                true, " Academic Administrator ", " ADMIN@UNIVERSITY.TEST ",
                "StrongPassword123!"));

        initializer.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User user = captor.getValue();
        assertEquals("Academic Administrator", user.getName());
        assertEquals("admin@university.test", user.getEmail());
        assertEquals("bcrypt-hash", user.getPasswordHash());
        assertEquals(Role.ADMIN, user.getRole());
        assertTrue(user.isEmailVerified());
        assertTrue(user.isEnabled());
    }

    private InitialAdminInitializer initializer(InitialAdminProperties properties) {
        return new InitialAdminInitializer(properties, userRepository, passwordEncoder);
    }
}
