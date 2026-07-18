package com.uni.ms.user;

import java.util.Optional;

public interface UserDirectory {
    Optional<User> findById(Long id);
}
