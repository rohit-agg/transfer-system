package com.transfer.application.services.transactions;

import com.transfer.application.dtos.transactions.SubmitTransaction;
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

    public void submitTransaction(SubmitTransaction submitTransaction) {

        if (submitTransaction.getSourceAccountId().equals(submitTransaction.getDestinationAccountId())) {
            logger.error("Source and destination accounts cannot be the same, account id = {}", submitTransaction.getSourceAccountId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source and destination accounts cannot be the same");
        }

        Account sourceAccount = this.accountRepository.findAccountByAccountId(submitTransaction.getSourceAccountId());
        if (sourceAccount == null) {
            logger.error("Source account not found, account id = {}", submitTransaction.getSourceAccountId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Source account not found");
        } else if (sourceAccount.getBalance() < submitTransaction.getAmount()) {
            logger.error("Insufficient funds, account id = {}, balance = {}", submitTransaction.getSourceAccountId(), sourceAccount.getBalance());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient funds");
        }

        Account destinationAccount = this.accountRepository.findAccountByAccountId(submitTransaction.getDestinationAccountId());
        if (destinationAccount == null) {
            logger.error("Destination account not found, account id = {}", submitTransaction.getDestinationAccountId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination account not found");
        }

        try {

            Boolean result = this.transactionTemplate.execute(status -> executeTransaction(status, sourceAccount, destinationAccount, submitTransaction));
            if (result == null || !result) {
                logger.error("Transaction failed, account id = {}", submitTransaction.getSourceAccountId());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Transaction failed");
            }

            logger.info("Transaction completed, source account id = {}, destination account id = {}", submitTransaction.getSourceAccountId(), submitTransaction.getDestinationAccountId());

        } catch (Exception e) {
            logger.error("Transaction failed, account id = {}, error = {}", submitTransaction.getSourceAccountId(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Transaction failed");
        }
    }

    private Boolean executeTransaction(TransactionStatus status, Account sourceAccount, Account destinationAccount, SubmitTransaction submitTransaction) {

        UUID transactionId = UUID.randomUUID();

        Ledger debitEntry = Ledger.builder()
                .transactionId(transactionId)
                .accountId(sourceAccount.getAccountId())
                .debit(submitTransaction.getAmount())
                .startBalance(sourceAccount.getBalance())
                .build();
        debitEntry = this.ledgerRepository.save(debitEntry);
        logger.info("Debit entry created, ledger id = {}", debitEntry.getId());

        Integer debitResult = this.accountRepository.debitBalance(sourceAccount.getId(), submitTransaction.getAmount());
        if (debitResult == 0) {
            logger.error("Debit failed from source account, account id = {}", sourceAccount.getAccountId());
            status.setRollbackOnly();
            return false;
        }
        logger.info("Debit completed from source account, account id = {}", sourceAccount.getAccountId());

        sourceAccount = this.accountRepository.findById(sourceAccount.getId()).get();

        debitEntry.setEndBalance(sourceAccount.getBalance());
        debitEntry.setStatus(Ledger.Status.COMPLETED);
        this.ledgerRepository.save(debitEntry);
        logger.info("Debit entry marked as complete, ledger id = {}", debitEntry.getId());

        Ledger creditEntry = Ledger.builder()
                .transactionId(transactionId)
                .accountId(destinationAccount.getAccountId())
                .credit(submitTransaction.getAmount())
                .startBalance(destinationAccount.getBalance())
                .build();
        creditEntry = this.ledgerRepository.save(creditEntry);
        logger.info("Credit entry created, ledger id = {}", creditEntry.getId());

        Integer creditResult = this.accountRepository.creditBalance(destinationAccount.getId(), submitTransaction.getAmount());
        if (creditResult == 0) {
            logger.error("Credit failed from destination account, account id = {}", destinationAccount.getAccountId());
            status.setRollbackOnly();
            return false;
        }
        logger.info("Credit completed from destination account, account id = {}", destinationAccount.getAccountId());

        destinationAccount = this.accountRepository.findById(destinationAccount.getId()).get();

        creditEntry.setEndBalance(destinationAccount.getBalance());
        creditEntry.setStatus(Ledger.Status.COMPLETED);
        this.ledgerRepository.save(creditEntry);
        logger.info("Credit entry marked as complete, ledger id = {}", creditEntry.getId());

        return true;
    }
}
