package com.transfer.application.services.transactions;

import com.transfer.application.dtos.transactions.SubmitTransaction;
import com.transfer.application.repositories.accounts.Account;
import com.transfer.application.repositories.accounts.AccountRepository;
import com.transfer.application.repositories.ledgers.Ledger;
import com.transfer.application.repositories.ledgers.LedgerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private LedgerRepository ledgerRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private void mockExecuteRunsCallback() {
        when(transactionTemplate.execute(any()))
                .thenAnswer(invocation -> {

                    TransactionCallback<Boolean> callback = (TransactionCallback<Boolean>) invocation.getArgument(0);
                    TransactionStatus status = mock(TransactionStatus.class);
                    return callback.doInTransaction(status);
                });
    }

    @Test
    @DisplayName("submitTransaction: throws NOT_FOUND when source account missing")
    void submitTransaction_sourceMissing_throwsNotFound() {

        SubmitTransaction req = new SubmitTransaction();
        req.setSourceAccountId(1L);
        req.setDestinationAccountId(2L);
        req.setAmount(100.0);

        when(accountRepository.findAccountByAccountId(1L)).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> transactionService.submitTransaction(req));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(accountRepository, never()).findAccountByAccountId(2L);
        verifyNoInteractions(ledgerRepository);
        verifyNoInteractions(transactionTemplate);
    }

    @Test
    @DisplayName("submitTransaction: throws NOT_FOUND when destination account missing")
    void submitTransaction_destinationMissing_throwsNotFound() {

        SubmitTransaction req = new SubmitTransaction();
        req.setSourceAccountId(1L);
        req.setDestinationAccountId(2L);
        req.setAmount(100.0);

        Account source = new Account();
        source.setId(10L);
        source.setAccountId(1L);
        source.setBalance(1000.0);
        when(accountRepository.findAccountByAccountId(1L)).thenReturn(source);
        when(accountRepository.findAccountByAccountId(2L)).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> transactionService.submitTransaction(req));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verifyNoInteractions(ledgerRepository);
        verifyNoInteractions(transactionTemplate);
    }

    @Test
    @DisplayName("submitTransaction: successful flow debits source, credits destination, and completes ledgers")
    void submitTransaction_success() {

        mockExecuteRunsCallback();

        Account source = new Account();
        source.setId(10L);
        source.setAccountId(1001L);
        source.setBalance(1000.0);

        Account destination = new Account();
        destination.setId(20L);
        destination.setAccountId(2002L);
        destination.setBalance(500.0);

        when(accountRepository.findAccountByAccountId(1001L)).thenReturn(source);
        when(accountRepository.findAccountByAccountId(2002L)).thenReturn(destination);

        when(ledgerRepository.save(any(Ledger.class))).thenAnswer(invocation -> {
            Ledger l = invocation.getArgument(0);
            if (l.getId() == null) {
                l.setId((long) (Math.random() * 1000 + 1));
            }
            return l;
        });

        when(accountRepository.debitBalance(10L, 100.0)).thenReturn(1);
        when(accountRepository.creditBalance(20L, 100.0)).thenReturn(1);

        Account sourceAfter = new Account();
        sourceAfter.setId(10L);
        sourceAfter.setAccountId(1001L);
        sourceAfter.setBalance(900.0);

        Account destinationAfter = new Account();
        destinationAfter.setId(20L);
        destinationAfter.setAccountId(2002L);
        destinationAfter.setBalance(600.0);

        when(accountRepository.findById(10L)).thenReturn(Optional.of(sourceAfter));
        when(accountRepository.findById(20L)).thenReturn(Optional.of(destinationAfter));

        SubmitTransaction req = new SubmitTransaction();
        req.setSourceAccountId(1001L);
        req.setDestinationAccountId(2002L);
        req.setAmount(100.0);

        assertDoesNotThrow(() -> transactionService.submitTransaction(req));

        ArgumentCaptor<Ledger> ledgerCaptor = ArgumentCaptor.forClass(Ledger.class);
        verify(ledgerRepository, atLeast(4)).save(ledgerCaptor.capture());

        boolean hasCompletedStatus = ledgerCaptor.getAllValues().stream().anyMatch(l -> l.getStatus() == Ledger.Status.COMPLETED);
        assertTrue(hasCompletedStatus, "At least one ledger should be marked COMPLETED");

        verify(accountRepository).debitBalance(10L, 100.0);
        verify(accountRepository).creditBalance(20L, 100.0);
    }

    @Test
    @DisplayName("submitTransaction: debit failure triggers rollback and INTERNAL_SERVER_ERROR")
    void submitTransaction_debitFailure_throwsISE() {
        mockExecuteRunsCallback();

        Account source = new Account();
        source.setId(10L);
        source.setAccountId(1001L);
        source.setBalance(1000.0);
        Account destination = new Account();
        destination.setId(20L);
        destination.setAccountId(2002L);
        destination.setBalance(500.0);

        when(accountRepository.findAccountByAccountId(1001L)).thenReturn(source);
        when(accountRepository.findAccountByAccountId(2002L)).thenReturn(destination);

        when(ledgerRepository.save(any(Ledger.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.debitBalance(10L, 100.0)).thenReturn(0);

        SubmitTransaction req = new SubmitTransaction();
        req.setSourceAccountId(1001L);
        req.setDestinationAccountId(2002L);
        req.setAmount(100.0);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> transactionService.submitTransaction(req));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());

        verify(accountRepository, never()).creditBalance(anyLong(), anyDouble());
    }

    @Test
    @DisplayName("submitTransaction: credit failure triggers rollback and INTERNAL_SERVER_ERROR")
    void submitTransaction_creditFailure_throwsISE() {
        mockExecuteRunsCallback();

        Account source = new Account();
        source.setId(10L);
        source.setAccountId(1001L);
        source.setBalance(1000.0);
        Account destination = new Account();
        destination.setId(20L);
        destination.setAccountId(2002L);
        destination.setBalance(500.0);

        when(accountRepository.findAccountByAccountId(1001L)).thenReturn(source);
        when(accountRepository.findAccountByAccountId(2002L)).thenReturn(destination);

        when(ledgerRepository.save(any(Ledger.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(accountRepository.debitBalance(10L, 100.0)).thenReturn(1);
        Account sourceAfter = new Account();
        sourceAfter.setId(10L);
        sourceAfter.setAccountId(1001L);
        sourceAfter.setBalance(900.0);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(sourceAfter));

        when(accountRepository.creditBalance(20L, 100.0)).thenReturn(0);

        SubmitTransaction req = new SubmitTransaction();
        req.setSourceAccountId(1001L);
        req.setDestinationAccountId(2002L);
        req.setAmount(100.0);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> transactionService.submitTransaction(req));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());

        verify(accountRepository).debitBalance(10L, 100.0);
        verify(accountRepository).creditBalance(20L, 100.0);
    }
}
