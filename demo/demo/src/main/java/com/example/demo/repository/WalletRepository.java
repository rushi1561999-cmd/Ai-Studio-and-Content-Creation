package com.example.demo.repository;

import com.example.demo.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select wallet from Wallet wallet where wallet.workspaceId = :workspaceId")
    Optional<Wallet> findByIdForUpdate(String workspaceId);
}
