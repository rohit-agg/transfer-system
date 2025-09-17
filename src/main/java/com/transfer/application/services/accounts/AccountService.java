package com.transfer.application.services.accounts;

import com.transfer.application.dtos.accounts.AccountInfo;
import com.transfer.application.dtos.accounts.CreateAccount;
import com.transfer.application.repositories.accounts.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    public Integer createAccount(CreateAccount createAccount) {
        return 0;
    }

    public AccountInfo getAccountDetails(Integer accountId) {
    }
}
