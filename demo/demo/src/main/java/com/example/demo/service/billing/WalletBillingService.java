package com.example.demo.service.billing;

import com.example.demo.entity.CreditTransaction;
import com.example.demo.entity.Wallet;
import com.example.demo.enums.CreditTransactionType;
import com.example.demo.repository.CreditTransactionRepository;
import com.example.demo.repository.WalletRepository;
import com.example.demo.service.audit.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WalletBillingService {

    public static final int DEFAULT_FREE_CREDITS = 5;

    private final WalletRepository walletRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final AuditService auditService;

    public WalletBillingService(
            WalletRepository walletRepository,
            CreditTransactionRepository creditTransactionRepository,
            AuditService auditService) {
        this.walletRepository = walletRepository;
        this.creditTransactionRepository = creditTransactionRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Wallet getOrCreateWallet(String workspaceId) {
        return walletRepository.findById(workspaceId).orElseGet(() -> {
            Wallet wallet = new Wallet(workspaceId, DEFAULT_FREE_CREDITS);
            Wallet saved = walletRepository.save(wallet);
            recordTransaction(workspaceId, DEFAULT_FREE_CREDITS, CreditTransactionType.CREDIT,
                    null, "Initial free credits", DEFAULT_FREE_CREDITS);
            return saved;
        });
    }

    @Transactional
    public Wallet credit(String workspaceId, int amount, CreditTransactionType type, String referenceId, String description) {
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credit amount must be positive.");
        }
        Wallet wallet = getOrCreateWallet(workspaceId);
        wallet.setCredits(wallet.getCredits() + amount);
        Wallet saved = walletRepository.save(wallet);
        recordTransaction(workspaceId, amount, type, referenceId, description, saved.getCredits());
        auditService.log(null, workspaceId, "WALLET_CREDIT", "Wallet", workspaceId,
                "{\"amount\":" + amount + ",\"type\":\"" + type + "\"}");
        return saved;
    }

    @Transactional
    public Wallet debit(String workspaceId, int amount, String referenceId, String description) {
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debit amount must be positive.");
        }
        Wallet wallet = walletRepository.findById(workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Insufficient credits."));
        if (wallet.getCredits() < amount) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Insufficient credits.");
        }
        wallet.setCredits(wallet.getCredits() - amount);
        Wallet saved = walletRepository.save(wallet);
        recordTransaction(workspaceId, -amount, CreditTransactionType.DEBIT, referenceId, description, saved.getCredits());
        auditService.log(null, workspaceId, "WALLET_DEBIT", "Wallet", workspaceId,
                "{\"amount\":" + amount + "}");
        return saved;
    }

    private void recordTransaction(
            String workspaceId,
            int amount,
            CreditTransactionType type,
            String referenceId,
            String description,
            int balanceAfter) {
        CreditTransaction tx = new CreditTransaction();
        tx.setWorkspaceId(workspaceId);
        tx.setAmount(amount);
        tx.setType(type);
        tx.setReferenceId(referenceId);
        tx.setDescription(description);
        tx.setBalanceAfter(balanceAfter);
        creditTransactionRepository.save(tx);
    }
}
