package com.example.tier4.account;

import com.example.tier4.ledger.LedgerRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final LedgerRepository ledgerRepository;

    public AccountService(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    @Cacheable(value = "balance", key = "#accountId")
    @Transactional(readOnly = true)
    public Long getBalance(Long accountId) {
        System.out.println("CACHE MISS - calculating SUM for account " + accountId);
        return ledgerRepository.getBalance(accountId);
    }

}