package com.transfer.application.services.transactions;

import com.transfer.application.dtos.transactions.SubmitTransaction;
import com.transfer.application.dtos.transactions.TransactionSuccess;
import com.transfer.application.repositories.accounts.Account;
import com.transfer.application.repositories.accounts.AccountRepository;
import com.transfer.application.repositories.ledgers.Ledger;
import com.transfer.application.repositories.ledgers.LedgerRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class TransactionService {

    private static final Logger logger = LogManager.getLogger();

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LedgerRepository ledgerRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    public TransactionSuccess submitTransaction(SubmitTransaction submitTransaction) {

        // Check if source and destination account are same, raise error otherwise
        if (submitTransaction.getSourceAccountId().equals(submitTransaction.getDestinationAccountId())) {
            logger.error("Source and destination accounts cannot be the same, account id = {}", submitTransaction.getSourceAccountId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source and destination accounts cannot be the same");
        }

        // Check if source account exists and have enough balance for the transaction, raise error otherwise
        Account sourceAccount = this.accountRepository.findAccountByAccountId(submitTransaction.getSourceAccountId());
        if (sourceAccount == null) {
            logger.error("Source account not found, account id = {}", submitTransaction.getSourceAccountId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Source account not found");
        } else if (sourceAccount.getBalance() < submitTransaction.getAmount()) {
            logger.error("Insufficient funds, account id = {}, balance = {}", submitTransaction.getSourceAccountId(), sourceAccount.getBalance());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient funds");
        }

        // Check if destination account exists, raise error otherwise
        Account destinationAccount = this.accountRepository.findAccountByAccountId(submitTransaction.getDestinationAccountId());
        if (destinationAccount == null) {
            logger.error("Destination account not found, account id = {}", submitTransaction.getDestinationAccountId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination account not found");
        }

        try {

            // Execute within a transaction, both debit from source and credit to destination should be completed
            // Raise error otherwise
            Boolean result = this.transactionTemplate.execute(status -> executeTransaction(status, sourceAccount, destinationAccount, submitTransaction));
            if (result == null || !result) {
                logger.error("Transaction failed, account id = {}", submitTransaction.getSourceAccountId());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Transaction failed");
            }

            // Return successful response
            logger.info("Transaction completed, source account id = {}, destination account id = {}", submitTransaction.getSourceAccountId(), submitTransaction.getDestinationAccountId());
            return TransactionSuccess.builder()
                    .sourceAccountId(sourceAccount.getAccountId())
                    .updatedBalance(sourceAccount.getBalance())
                    .build();

        } catch (Exception e) {
            // If an error occurs during the execution, raise the same
            logger.error("Transaction failed, account id = {}, error = {}", submitTransaction.getSourceAccountId(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Transaction failed");
        }
    }

    private Boolean executeTransaction(TransactionStatus status, Account sourceAccount, Account destinationAccount, SubmitTransaction submitTransaction) {

        UUID transactionId = UUID.randomUUID();

        // Create a Debit Ledger entry for source account
        Ledger debitEntry = Ledger.builder()
                .transactionId(transactionId)
                .accountId(sourceAccount.getAccountId())
                .debit(submitTransaction.getAmount())
                .startBalance(sourceAccount.getBalance())
                .status(Ledger.Status.IN_PROGRESS)
                .build();
        debitEntry = this.ledgerRepository.save(debitEntry);
        logger.info("Debit entry created, ledger id = {}", debitEntry.getId());

        // Debit the amount from source account
        Integer debitResult = this.accountRepository.debitBalance(sourceAccount.getId(), submitTransaction.getAmount());
        if (debitResult == 0) {
            // If nothing was updated in DB, a concurrent transaction updated the balance and now account has insufficient funds
            logger.error("Debit failed from source account, account id = {}", sourceAccount.getAccountId());
            status.setRollbackOnly();
            return false;
        }
        logger.info("Debit completed from source account, account id = {}", sourceAccount.getAccountId());

        sourceAccount = this.accountRepository.findById(sourceAccount.getId()).get();

        // Update Debit Ledger to reflect successful debit
        debitEntry.setEndBalance(sourceAccount.getBalance());
        debitEntry.setStatus(Ledger.Status.COMPLETED);
        this.ledgerRepository.save(debitEntry);
        logger.info("Debit entry marked as complete, ledger id = {}", debitEntry.getId());

        // Create a Credit Ledger entry for destination account
        Ledger creditEntry = Ledger.builder()
                .transactionId(transactionId)
                .accountId(destinationAccount.getAccountId())
                .credit(submitTransaction.getAmount())
                .startBalance(destinationAccount.getBalance())
                .status(Ledger.Status.IN_PROGRESS)
                .build();
        creditEntry = this.ledgerRepository.save(creditEntry);
        logger.info("Credit entry created, ledger id = {}", creditEntry.getId());

        // Credit the amount into destination account
        Integer creditResult = this.accountRepository.creditBalance(destinationAccount.getId(), submitTransaction.getAmount());
        if (creditResult == 0) {
            // If nothing was updated in DB, some issue has occurred with account
            logger.error("Credit failed from destination account, account id = {}", destinationAccount.getAccountId());
            status.setRollbackOnly();
            return false;
        }
        logger.info("Credit completed from destination account, account id = {}", destinationAccount.getAccountId());

        destinationAccount = this.accountRepository.findById(destinationAccount.getId()).get();

        // Update Credit Ledger to reflect successful credit
        creditEntry.setEndBalance(destinationAccount.getBalance());
        creditEntry.setStatus(Ledger.Status.COMPLETED);
        this.ledgerRepository.save(creditEntry);
        logger.info("Credit entry marked as complete, ledger id = {}", creditEntry.getId());

        return true;
    }
}
