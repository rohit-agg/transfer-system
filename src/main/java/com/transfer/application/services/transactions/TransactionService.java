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

        Account sourceAccount = this.accountRepository.findAccountByAccountId(submitTransaction.getSourceAccountId());
        if (sourceAccount == null) {
            logger.error("Source account not found, account id = {}", submitTransaction.getSourceAccountId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Source account not found");
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

        Ledger debitEntry = new Ledger();
        debitEntry.setAccountId(sourceAccount.getAccountId());
        debitEntry.setDebit(submitTransaction.getAmount());
        debitEntry.setStartBalance(sourceAccount.getBalance());
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

        Ledger creditEntry = new Ledger();
        creditEntry.setAccountId(destinationAccount.getAccountId());
        creditEntry.setCredit(submitTransaction.getAmount());
        creditEntry.setStartBalance(destinationAccount.getBalance());
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
