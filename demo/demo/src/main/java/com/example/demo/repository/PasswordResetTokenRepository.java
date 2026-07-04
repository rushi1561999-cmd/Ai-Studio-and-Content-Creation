package com.example.demo.repository;

import com.example.demo.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {
    Optional<PasswordResetToken> findByTokenAndUsedFalse(String token);
    void deleteByUserId(String userId);
}
