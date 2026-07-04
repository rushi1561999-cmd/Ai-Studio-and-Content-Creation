package com.example.demo.repository;

import com.example.demo.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, String> { // <--- MUST BE 'String'
    // You don't even need to write any methods inside here!
    // JpaRepository gives you findById() for free.
}