package com.uni.ms.identity.infrastructure;

import com.uni.ms.identity.application.LoginAttemptPolicy;
import com.uni.ms.identity.application.LoginAttemptProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class InMemoryLoginAttemptPolicy implements LoginAttemptPolicy {

    private final LoginAttemptProperties properties;
    private final ConcurrentHashMap<AttemptKey, AttemptState> attempts = new ConcurrentHashMap<>();

    @Override
    public boolean isBlocked(String normalizedEmail, String clientIp) {
        AttemptKey key = new AttemptKey(normalizedEmail, clientIp);
        AttemptState state = attempts.get(key);
        if (state == null) {
            return false;
        }
        if (state.windowStartedAt().plus(properties.window()).isBefore(Instant.now())) {
            attempts.remove(key, state);
            return false;
        }
        return state.failures() >= properties.maxFailures();
    }

    @Override
    public void recordFailure(String normalizedEmail, String clientIp) {
        AttemptKey key = new AttemptKey(normalizedEmail, clientIp);
        Instant now = Instant.now();
        attempts.compute(key, (ignored, current) -> {
            if (current == null || current.windowStartedAt().plus(properties.window()).isBefore(now)) {
                return new AttemptState(1, now);
            }
            return new AttemptState(current.failures() + 1, current.windowStartedAt());
        });
    }

    @Override
    public void reset(String normalizedEmail, String clientIp) {
        attempts.remove(new AttemptKey(normalizedEmail, clientIp));
    }

    private record AttemptKey(String normalizedEmail, String clientIp) {
    }

    private record AttemptState(int failures, Instant windowStartedAt) {
    }
}
