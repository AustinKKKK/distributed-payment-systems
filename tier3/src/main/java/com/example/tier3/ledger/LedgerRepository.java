package com.example.tier3.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {

    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM LedgerEntry l WHERE l.accountId = :accountId")
    Long getBalance(@Param("accountId") Long accountId);

}