package com.uni.ms.identity.application;

public interface LoginAttemptPolicy {

    boolean isBlocked(String normalizedEmail, String clientIp);

    void recordFailure(String normalizedEmail, String clientIp);

    void reset(String normalizedEmail, String clientIp);
}
