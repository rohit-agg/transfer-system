package com.transfer.application.repositories.accounts;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Account findAccountByAccountId(Long accountId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "UPDATE Account SET balance = balance - ?2 WHERE id = ?1 AND balance > ?2")
    int debitBalance(Long id, Double balance);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "UPDATE Account SET balance = balance + ?2 WHERE id = ?1")
    int creditBalance(Long id, Double balance);
}
