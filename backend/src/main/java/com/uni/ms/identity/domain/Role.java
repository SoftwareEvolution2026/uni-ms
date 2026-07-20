package com.uni.ms.identity.domain;

public enum Role {
    ADMIN,
    ACADEMIC_MANAGER;

    public String authority() {
        return "ROLE_" + name();
    }
}
