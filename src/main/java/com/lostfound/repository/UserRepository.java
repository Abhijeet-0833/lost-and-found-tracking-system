package com.lostfound.repository;

import com.lostfound.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // This allows the "backend" to find a user by their email for OTP
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
}