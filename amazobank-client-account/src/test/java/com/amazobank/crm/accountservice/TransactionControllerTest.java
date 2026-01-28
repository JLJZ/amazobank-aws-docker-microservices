package com.amazobank.crm.accountservice;

import com.amazobank.crm.accountservice.api.TransactionController;
import com.amazobank.crm.accountservice.domain.Account;
import com.amazobank.crm.accountservice.domain.AccountStatus;
import com.amazobank.crm.accountservice.domain.AccountType;
import com.amazobank.crm.accountservice.domain.Transaction;
import com.amazobank.crm.accountservice.domain.TransactionStatus;
import com.amazobank.crm.accountservice.domain.TransactionType;
import com.amazobank.crm.accountservice.security.SecurityConfig;
import com.amazobank.crm.accountservice.service.AccountService;
import com.amazobank.crm.accountservice.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private TransactionService transactionService;

    private Account buildAccount(String accountId, String agentId) {
        return Account.builder()
                .accountId(accountId)
                .clientId(UUID.randomUUID().toString())
                .agentId(agentId)
                .accountType(AccountType.Savings)
                .accountStatus(AccountStatus.Active)
                .openingDate(LocalDate.now())
                .initialDeposit(100.0)
                .currency("USD")
                .branchId("branch")
                .build();
    }

    private Transaction buildTransaction(String transactionId, Account account) {
        return Transaction.builder()
                .transactionId(transactionId)
                .clientId(account.getClientId())
                .account(account)
                .transactionType(TransactionType.D)
                .amount(50.0)
                .date(LocalDate.now())
                .status(TransactionStatus.Completed)
                .build();
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = {"AGENT"})
    void getTransactions_whenAccountHasTransactions_returnsList() throws Exception {
        String agentId = "11111111-1111-1111-1111-111111111111";
        String accountId = UUID.randomUUID().toString();
        Account account = buildAccount(accountId, agentId);
        Transaction tx1 = buildTransaction(UUID.randomUUID().toString(), account);
        Transaction tx2 = buildTransaction(UUID.randomUUID().toString(), account);

        when(accountService.findById(accountId)).thenReturn(Optional.of(account));
        when(transactionService.findByAccountId(accountId)).thenReturn(List.of(tx1, tx2));

        mockMvc.perform(get("/api/accounts/{accountId}/transactions", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionId").value(tx1.getTransactionId()))
                .andExpect(jsonPath("$[0].accountId").value(accountId))
                .andExpect(jsonPath("$[1].transactionId").value(tx2.getTransactionId()))
                .andExpect(jsonPath("$[1].accountId").value(accountId));

        verify(accountService).findById(accountId);
        verify(transactionService).findByAccountId(accountId);
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = {"AGENT"})
    void getTransactions_whenAccountHasNoTransactions_returnsEmptyList() throws Exception {
        String agentId = "11111111-1111-1111-1111-111111111111";
        String accountId = UUID.randomUUID().toString();
        Account account = buildAccount(accountId, agentId);

        when(accountService.findById(accountId)).thenReturn(Optional.of(account));
        when(transactionService.findByAccountId(accountId)).thenReturn(List.of());

        mockMvc.perform(get("/api/accounts/{accountId}/transactions", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(accountService).findById(accountId);
        verify(transactionService).findByAccountId(accountId);
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = {"AGENT"})
    void getTransactions_whenAccountMissing_returns404() throws Exception {
        String accountId = UUID.randomUUID().toString();
        when(accountService.findById(accountId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/accounts/{accountId}/transactions", accountId))
                .andExpect(status().isNotFound());

        verify(accountService).findById(accountId);
        verify(transactionService, never()).findByAccountId(anyString());
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = {"AGENT"})
    void getTransactions_whenAccountBelongsToDifferentAgent_returns403() throws Exception {
        String accountId = UUID.randomUUID().toString();
        Account account = buildAccount(accountId, "22222222-2222-2222-2222-222222222222");

        when(accountService.findById(accountId)).thenReturn(Optional.of(account));

        mockMvc.perform(get("/api/accounts/{accountId}/transactions", accountId))
                .andExpect(status().isForbidden());

        verify(accountService).findById(accountId);
        verify(transactionService, never()).findByAccountId(anyString());
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = {"AGENT"})
    void getTransaction_whenFound_returnsTransaction() throws Exception {
        String agentId = "11111111-1111-1111-1111-111111111111";
        String accountId = UUID.randomUUID().toString();
        String transactionId = UUID.randomUUID().toString();
        Account account = buildAccount(accountId, agentId);
        Transaction transaction = buildTransaction(transactionId, account);

        when(accountService.findById(accountId)).thenReturn(Optional.of(account));
        when(transactionService.findByAccountIdAndTransactionId(accountId, transactionId))
                .thenReturn(Optional.of(transaction));

        mockMvc.perform(get("/api/accounts/{accountId}/transactions/{transactionId}", accountId, transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(transactionId))
                .andExpect(jsonPath("$.accountId").value(accountId));

        verify(accountService).findById(accountId);
        verify(transactionService).findByAccountIdAndTransactionId(accountId, transactionId);
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = {"AGENT"})
    void getTransaction_whenNotFound_returns404() throws Exception {
        String agentId = "11111111-1111-1111-1111-111111111111";
        String accountId = UUID.randomUUID().toString();
        Account account = buildAccount(accountId, agentId);

        when(accountService.findById(accountId)).thenReturn(Optional.of(account));
        when(transactionService.findByAccountIdAndTransactionId(accountId, "tx"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/accounts/{accountId}/transactions/{transactionId}", accountId, "tx"))
                .andExpect(status().isNotFound());

        verify(accountService).findById(accountId);
        verify(transactionService).findByAccountIdAndTransactionId(accountId, "tx");
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = {"AGENT"})
    void getTransaction_whenAccountMissing_returns404() throws Exception {
        String accountId = UUID.randomUUID().toString();
        when(accountService.findById(accountId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/accounts/{accountId}/transactions/{transactionId}", accountId, "tx"))
                .andExpect(status().isNotFound());

        verify(accountService).findById(accountId);
        verify(transactionService, never()).findByAccountIdAndTransactionId(anyString(), anyString());
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = {"AGENT"})
    void getTransaction_whenAccountBelongsToDifferentAgent_returns403() throws Exception {
        String accountId = UUID.randomUUID().toString();
        Account account = buildAccount(accountId, "22222222-2222-2222-2222-222222222222");
        when(accountService.findById(accountId)).thenReturn(Optional.of(account));

        mockMvc.perform(get("/api/accounts/{accountId}/transactions/{transactionId}", accountId, "tx"))
                .andExpect(status().isForbidden());

        verify(accountService).findById(accountId);
        verify(transactionService, never()).findByAccountIdAndTransactionId(anyString(), anyString());
    }
}
