package com.transfer.application.controllers;

import com.transfer.application.dtos.accounts.AccountInfo;
import com.transfer.application.dtos.accounts.CreateAccount;
import com.transfer.application.services.accounts.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;

@Controller
@RequestMapping("/accounts")
public class AccountsController {

    @Autowired
    private AccountService accountService;

    @PostMapping()
    public ResponseEntity<Void> createAccount(@RequestBody CreateAccount createAccount) {

        Integer accountId = this.accountService.createAccount(createAccount);
        return ResponseEntity.created(URI.create("/accounts/" + accountId))
                .build();
    }

    @GetMapping(":accountId")
    public ResponseEntity<AccountInfo> getAccountDetails(@RequestParam("accountId") Integer accountId) {

        AccountInfo accountInfo = this.accountService.getAccountDetails(accountId);
        return ResponseEntity.ok()
                .body(accountInfo);
    }
}
