package com.transfer.application.services.accounts;

import com.transfer.application.dtos.accounts.AccountInfo;
import com.transfer.application.dtos.accounts.CreateAccount;
import com.transfer.application.repositories.accounts.Account;
import com.transfer.application.repositories.accounts.AccountRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {

    private static final Logger logger = LogManager.getLogger();

    @Autowired
    private AccountRepository accountRepository;

    public Long createAccount(CreateAccount createAccount) {

        Account accountFound = this.accountRepository.findAccountByAccountId(createAccount.getAccountId());
        if (accountFound != null) {
            logger.error("Account already exists, account id = {}", createAccount.getAccountId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account already exists");
        }

        Account account = new Account();
        account.setAccountId(createAccount.getAccountId());
        account.setName(createAccount.getName());
        account.setBalance(createAccount.getInitialBalance());
        account = this.accountRepository.save(account);
        logger.info("Account created, account id = {}", account.getAccountId());

        return account.getId();
    }

    public AccountInfo getAccountDetails(Long accountId) {

        Account account = this.accountRepository.findAccountByAccountId(accountId);
        if (account == null) {
            logger.error("Account not found, account id = {}", accountId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
        }

        AccountInfo accountInfo = AccountInfo.builder()
                .accountId(account.getAccountId())
                .name(account.getName())
                .balance(account.getBalance())
                .build();
        logger.info("Account details retrieved, account id = {}", accountId);

        return accountInfo;
    }
}
