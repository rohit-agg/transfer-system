package com.transfer.application.services.accounts;

import com.transfer.application.dtos.accounts.AccountInfo;
import com.transfer.application.dtos.accounts.CreateAccount;
import com.transfer.application.repositories.accounts.Account;
import com.transfer.application.repositories.accounts.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("createAccount: creates and returns id when accountId not present")
    void createAccount_success() {

        CreateAccount req = new CreateAccount();
        req.setAccountId(1001L);
        req.setName("Alice");
        req.setInitialBalance(250.0);

        when(accountRepository.findAccountByAccountId(1001L)).thenReturn(null);

        Account saved = new Account();
        saved.setId(42L);
        saved.setAccountId(1001L);
        saved.setName("Alice");
        saved.setBalance(250.0);
        when(accountRepository.save(any(Account.class))).thenReturn(saved);

        Long resultId = accountService.createAccount(req);

        assertEquals(42L, resultId);
        verify(accountRepository).findAccountByAccountId(1001L);

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account toSave = accountCaptor.getValue();
        assertEquals(1001L, toSave.getAccountId());
        assertEquals("Alice", toSave.getName());
        assertEquals(250.0, toSave.getBalance());
    }

    @Test
    @DisplayName("createAccount: throws BAD_REQUEST when account already exists")
    void createAccount_alreadyExists_throwsBadRequest() {

        CreateAccount req = new CreateAccount();
        req.setAccountId(2002L);
        req.setName("Bob");
        req.setInitialBalance(100.0);

        Account existing = new Account();
        existing.setId(1L);
        existing.setAccountId(2002L);
        when(accountRepository.findAccountByAccountId(2002L)).thenReturn(existing);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> accountService.createAccount(req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("getAccountDetails: returns mapped AccountInfo when account exists")
    void getAccountDetails_success() {

        Account account = new Account();
        account.setId(7L);
        account.setAccountId(3003L);
        account.setName("Charlie");
        account.setBalance(999.99);
        when(accountRepository.findAccountByAccountId(3003L)).thenReturn(account);

        AccountInfo info = accountService.getAccountDetails(3003L);

        assertNotNull(info);
        assertEquals(3003L, info.getAccountId());
        assertEquals("Charlie", info.getName());
        assertEquals(999.99, info.getBalance());
        verify(accountRepository).findAccountByAccountId(3003L);
    }

    @Test
    @DisplayName("getAccountDetails: throws NOT_FOUND when account is missing")
    void getAccountDetails_notFound_throwsNotFound() {

        when(accountRepository.findAccountByAccountId(4004L)).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> accountService.getAccountDetails(4004L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
