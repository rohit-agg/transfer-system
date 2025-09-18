package com.transfer.application.controllers;

import com.transfer.application.dtos.transactions.SubmitTransaction;
import com.transfer.application.dtos.transactions.TransactionSuccess;
import com.transfer.application.services.transactions.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/transactions")
public class TransactionsController {

    @Autowired
    private TransactionService transactionService;

    @PostMapping()
    public ResponseEntity<TransactionSuccess> submitTransaction(@Validated @RequestBody SubmitTransaction submitTransaction) {

        TransactionSuccess transactionSuccess = this.transactionService.submitTransaction(submitTransaction);
        return ResponseEntity.ok()
                .body(transactionSuccess);
    }
}
