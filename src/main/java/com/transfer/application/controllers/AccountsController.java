package com.transfer.application.controllers;

import com.transfer.application.dtos.accounts.AccountInfo;
import com.transfer.application.dtos.accounts.CreateAccount;
import com.transfer.application.services.accounts.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.URI;

@Controller
@RequestMapping("/accounts")
public class AccountsController {

    @Autowired
    private AccountService accountService;

    @PostMapping()
    public ResponseEntity<Void> createAccount(@Validated @RequestBody CreateAccount createAccount) {

        Long accountId = this.accountService.createAccount(createAccount);
        return ResponseEntity.created(URI.create("/accounts/" + accountId))
                .build();
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountInfo> getAccountDetails(@PathVariable("accountId") Long accountId) {

        AccountInfo accountInfo = this.accountService.getAccountDetails(accountId);
        return ResponseEntity.ok()
                .body(accountInfo);
    }
}
