package com.amazobank.crm.accountservice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.amazobank.crm.accountservice.api.AccountController;
import com.amazobank.crm.accountservice.domain.Account;
import com.amazobank.crm.accountservice.domain.AccountStatus;
import com.amazobank.crm.accountservice.domain.AccountType;
import com.amazobank.crm.accountservice.security.SecurityConfig;
import com.amazobank.crm.accountservice.service.AccountService;
import com.amazobank.crm.accountservice.service.SqsService;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
public class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @MockitoBean
    private AccountService service;
    
    @MockitoBean
    private SqsService sqsService;

    @BeforeEach
    void stubSqsNotifications() {
        doNothing().when(sqsService).sendEmailNotification(any(), any()); // Notification side-effect not needed for controller tests
    }

    // ========== GET /api/accounts Tests ==========

    @Test
    @WithMockUser(username = "a1b2c3d4-5678-90ab-cdef-111111111111", roles = {"AGENT"})
    void getAll_whenAccountsBelongToAgent_shouldReturnAccounts() throws Exception {
        // Arrange
        String agentId = "a1b2c3d4-5678-90ab-cdef-111111111111";
        UUID accountId1 = UUID.randomUUID();
        UUID accountId2 = UUID.randomUUID();
        UUID clientId1 = UUID.randomUUID();
        UUID clientId2 = UUID.randomUUID();
        
        Account account1 = Account.builder()
            .accountId(accountId1.toString())
            .clientId(clientId1.toString())
            .agentId(agentId.toString())
            .accountType(AccountType.Savings)
            .accountStatus(AccountStatus.Active)
            .openingDate(LocalDate.of(2024, 1, 15))
            .initialDeposit(5000.00)
            .currency("USD")
            .branchId("branch-001")
            .build();
        
        Account account2 = Account.builder()
            .accountId(accountId2.toString())
            .clientId(clientId2.toString())
            .agentId(agentId.toString())
            .accountType(AccountType.Checking)
            .accountStatus(AccountStatus.Active)
            .openingDate(LocalDate.of(2024, 2, 20))
            .initialDeposit(1000.00)
            .currency("USD")
            .branchId("branch-001")
            .build();
        
        when(service.findByAgentId(agentId)).thenReturn(List.of(account1, account2));
        
        // Act & Assert
        mockMvc.perform(get("/api/accounts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].accountId").value(accountId1.toString()))
            .andExpect(jsonPath("$[0].accountType").value("Savings"))
            .andExpect(jsonPath("$[0].agentId").value(agentId.toString()))
            .andExpect(jsonPath("$[1].accountId").value(accountId2.toString()))
            .andExpect(jsonPath("$[1].accountType").value("Checking"));
        
        verify(service).findByAgentId(agentId);
    }

    @Test
    @WithMockUser(username = "a1b2c3d4-5678-90ab-cdef-111111111111", roles = {"AGENT"})
    void getAll_withClientIdFilter_shouldReturnFilteredAccounts() throws Exception {
        // Arrange
        String agentId = "a1b2c3d4-5678-90ab-cdef-111111111111";
        UUID clientId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        
        Account account = Account.builder()
            .accountId(accountId.toString())
            .clientId(clientId.toString())
            .agentId(agentId)
            .accountType(AccountType.Savings)
            .accountStatus(AccountStatus.Active)
            .openingDate(LocalDate.of(2024, 1, 15))
            .initialDeposit(5000.00)
            .currency("USD")
            .branchId("branch-001")
            .build();
        
        when(service.findByAgentId(agentId)).thenReturn(List.of(account));
        
        // Act & Assert
        mockMvc.perform(get("/api/accounts")
                .param("clientId", clientId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].clientId").value(clientId.toString()));
        
        verify(service).findByAgentId(agentId);
    }

    @Test
    void getAll_withoutAuth_shouldReturn401() throws Exception {        
        mockMvc.perform(get("/api/accounts"))
            .andExpect(status().isUnauthorized());
        
        verify(service, never()).findByAgentId(any());
    }

    // ========== GET /api/accounts/{id} Tests ==========

    @Test
    @WithMockUser(username = "a1b2c3d4-5678-90ab-cdef-111111111111", roles = {"AGENT"})
    void getOne_whenAccountBelongsToAgent_shouldReturnAccount() throws Exception {
        // Arrange
        String accountId = "acc-001";
        String agentId = "a1b2c3d4-5678-90ab-cdef-111111111111";
        
        Account account = Account.builder()
            .accountId(accountId)
            .clientId("client-001")
            .agentId(agentId)
            .accountType(AccountType.Savings)
            .accountStatus(AccountStatus.Active)
            .openingDate(LocalDate.of(2024, 1, 15))
            .initialDeposit(5000.00)
            .currency("USD")
            .branchId("branch-001")
            .build();
        
        when(service.findById(accountId)).thenReturn(Optional.of(account));
        
        // Act & Assert
        mockMvc.perform(get("/api/accounts/{id}", accountId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountId").value(accountId))
            .andExpect(jsonPath("$.accountType").value("Savings"))
            .andExpect(jsonPath("$.agentId").value(agentId));
        
        verify(service).findById(accountId);
    }

    @Test
    @WithMockUser(username = "f9e8d7c6-1234-5678-90ab-222222222222", roles = {"AGENT"})
    void getOne_whenAccountBelongsToDifferentAgent_shouldReturn403() throws Exception {
        // Arrange
        String accountId = "acc-001";
        String ownerAgentId = "a1b2c3d4-5678-90ab-cdef-111111111111";
        
        Account account = Account.builder()
            .accountId(accountId)
            .clientId("client-001")
            .agentId(ownerAgentId)  // Belongs to different agent
            .accountType(AccountType.Savings)
            .accountStatus(AccountStatus.Active)
            .openingDate(LocalDate.of(2024, 1, 15))
            .initialDeposit(5000.00)
            .currency("USD")
            .branchId("branch-001")
            .build();
        
        when(service.findById(accountId)).thenReturn(Optional.of(account));
        
        // Act & Assert
        mockMvc.perform(get("/api/accounts/{id}", accountId))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("Forbidden"));
        
        verify(service).findById(accountId);
    }

    @Test
    @WithMockUser(username = "a1b2c3d4-5678-90ab-cdef-111111111111", roles = {"AGENT"})
    void getOne_whenAccountNotFound_shouldReturn404() throws Exception {
        // Arrange
        String accountId = "acc-nonexistent";
        
        when(service.findById(accountId)).thenReturn(Optional.empty());
        
        // Act & Assert
        mockMvc.perform(get("/api/accounts/{id}", accountId))
            .andExpect(status().isNotFound());
        
        verify(service).findById(accountId);
    }

    @Test
    void getOne_withoutAuth_shouldReturn401() throws Exception {
        String accountId = "acc-001";
        
        mockMvc.perform(get("/api/accounts/{id}", accountId))
            .andExpect(status().isUnauthorized());
        
        verify(service, never()).findById(any());
    }

    // ========== POST /api/accounts Tests ==========

    @Test
    @WithMockUser(username = "a1b2c3d4-5678-90ab-cdef-111111111111", roles = {"AGENT"})
    void create_withValidData_shouldCreateAccount() throws Exception {
        // Arrange
        String agentId = "a1b2c3d4-5678-90ab-cdef-111111111111";
        String requestBody = """
            {
                "clientId": "client-001",
                "accountType": "Savings",
                "initialDeposit": 5000.00,
                "currency": "USD",
                "branchId": "branch-001"
            }
            """;
        
        Account savedAccount = Account.builder()
            .accountId("acc-new-001")
            .clientId("client-001")
            .agentId(agentId)
            .accountType(AccountType.Savings)
            .accountStatus(AccountStatus.Active)
            .openingDate(LocalDate.now())
            .initialDeposit(5000.00)
            .currency("USD")
            .branchId("branch-001")
            .build();
        
        when(service.save(any(Account.class))).thenReturn(savedAccount);
        
        // Act & Assert
        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accountId").value("acc-new-001"))
            .andExpect(jsonPath("$.clientId").value("client-001"))
            .andExpect(jsonPath("$.accountType").value("Savings"))
            .andExpect(jsonPath("$.accountStatus").value("Active"))
            .andExpect(jsonPath("$.agentId").value(agentId));
        
        verify(service).save(any(Account.class));
    }

    @Test
    void create_withoutAuth_shouldReturn401() throws Exception {
        String requestBody = """
            {
                "clientId": "client-001",
                "accountType": "Savings",
                "initialDeposit": 5000.00,
                "currency": "USD",
                "branchId": "branch-001"
            }
            """;
        
        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isUnauthorized());
        
        verify(service, never()).save(any());
    }

    // ========== PUT /api/accounts/{id} Tests ==========

    @Test
    @WithMockUser(username = "a1b2c3d4-5678-90ab-cdef-111111111111", roles = {"AGENT"})
    void update_whenAccountBelongsToAgent_shouldUpdateAccount() throws Exception {
        // Arrange
        String accountId = "acc-001";
        String agentId = "a1b2c3d4-5678-90ab-cdef-111111111111";
        String requestBody = """
            {
                "accountStatus": "Inactive",
                "currency": "EUR"
            }
            """;
        
        Account existingAccount = Account.builder()
            .accountId(accountId)
            .clientId("client-001")
            .agentId(agentId)
            .accountType(AccountType.Savings)
            .accountStatus(AccountStatus.Active)
            .openingDate(LocalDate.of(2024, 1, 15))
            .initialDeposit(5000.00)
            .currency("USD")
            .branchId("branch-001")
            .build();
        
        Account updatedAccount = Account.builder()
            .accountId(accountId)
            .clientId("client-001")
            .agentId(agentId)
            .accountType(AccountType.Savings)
            .accountStatus(AccountStatus.Inactive)
            .openingDate(LocalDate.of(2024, 1, 15))
            .initialDeposit(5000.00)
            .currency("EUR")
            .branchId("branch-001")
            .build();
        
        when(service.findById(accountId)).thenReturn(Optional.of(existingAccount));
        when(service.save(any(Account.class))).thenReturn(updatedAccount);
        
        // Act & Assert
        mockMvc.perform(put("/api/accounts/{id}", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountStatus").value("Inactive"))
            .andExpect(jsonPath("$.currency").value("EUR"));
        
        verify(service).findById(accountId);
        verify(service).save(any(Account.class));
    }

    @Test
    @WithMockUser(username = "f9e8d7c6-1234-5678-90ab-222222222222", roles = {"AGENT"})
    void update_whenAccountBelongsToDifferentAgent_shouldReturn403() throws Exception {
        // Arrange
        String accountId = "acc-001";
        String ownerAgentId = "a1b2c3d4-5678-90ab-cdef-111111111111";
        String requestBody = """
            {
                "accountStatus": "Inactive"
            }
            """;
        
        Account account = Account.builder()
            .accountId(accountId)
            .clientId("client-001")
            .agentId(ownerAgentId)  // Belongs to different agent
            .accountType(AccountType.Savings)
            .accountStatus(AccountStatus.Active)
            .openingDate(LocalDate.of(2024, 1, 15))
            .initialDeposit(5000.00)
            .currency("USD")
            .branchId("branch-001")
            .build();
        
        when(service.findById(accountId)).thenReturn(Optional.of(account));
        
        // Act & Assert
        mockMvc.perform(put("/api/accounts/{id}", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("Forbidden"));
        
        verify(service).findById(accountId);
        verify(service, never()).save(any());
    }

    @Test
    @WithMockUser(username = "a1b2c3d4-5678-90ab-cdef-111111111111", roles = {"AGENT"})
    void update_whenAccountNotFound_shouldReturn404() throws Exception {
        // Arrange
        String accountId = "acc-nonexistent";
        String requestBody = """
            {
                "accountStatus": "Inactive"
            }
            """;
        
        when(service.findById(accountId)).thenReturn(Optional.empty());
        
        // Act & Assert
        mockMvc.perform(put("/api/accounts/{id}", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Not Found"));
        
        verify(service).findById(accountId);
        verify(service, never()).save(any());
    }

    @Test
    void update_withoutAuth_shouldReturn401() throws Exception {
        String accountId = "acc-001";
        String requestBody = """
            {
                "accountStatus": "Inactive"
            }
            """;
        
        mockMvc.perform(put("/api/accounts/{id}", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isUnauthorized());
        
        verify(service, never()).findById(any());
        verify(service, never()).save(any());
    }

    // ========== DELETE /api/accounts/{id} Tests ==========

    @Test
    @WithMockUser(username = "a1b2c3d4-5678-90ab-cdef-111111111111", roles = {"AGENT"})
    void delete_whenAccountBelongsToAgent_shouldSoftDeleteAccount() throws Exception {
        // Arrange
        String accountId = "acc-001";
        String agentId = "a1b2c3d4-5678-90ab-cdef-111111111111";
        
        Account account = Account.builder()
            .accountId(accountId)
            .clientId("client-001")
            .agentId(agentId)
            .accountType(AccountType.Savings)
            .accountStatus(AccountStatus.Active)
            .openingDate(LocalDate.of(2024, 1, 15))
            .initialDeposit(5000.00)
            .currency("USD")
            .branchId("branch-001")
            .build();
        
        when(service.findById(accountId)).thenReturn(Optional.of(account));
        
        // Act & Assert
        mockMvc.perform(delete("/api/accounts/{id}", accountId))
            .andExpect(status().isNoContent());
        
        verify(service).findById(accountId);
        verify(service).softDelete(account);
    }

    @Test
    @WithMockUser(username = "f9e8d7c6-1234-5678-90ab-222222222222", roles = {"AGENT"})
    void delete_whenAccountBelongsToDifferentAgent_shouldReturn403() throws Exception {
        // Arrange
        String accountId = "acc-001";
        String ownerAgentId = "a1b2c3d4-5678-90ab-cdef-111111111111";
        
        Account account = Account.builder()
            .accountId(accountId)
            .clientId("client-001")
            .agentId(ownerAgentId)  // Belongs to different agent
            .accountType(AccountType.Savings)
            .accountStatus(AccountStatus.Active)
            .openingDate(LocalDate.of(2024, 1, 15))
            .initialDeposit(5000.00)
            .currency("USD")
            .branchId("branch-001")
            .build();
        
        when(service.findById(accountId)).thenReturn(Optional.of(account));
        
        // Act & Assert
        mockMvc.perform(delete("/api/accounts/{id}", accountId))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("Forbidden"));
        
        verify(service).findById(accountId);
        verify(service, never()).save(any());
    }

    @Test
    @WithMockUser(username = "a1b2c3d4-5678-90ab-cdef-111111111111", roles = {"AGENT"})
    void delete_whenAccountNotFound_shouldReturn404() throws Exception {
        // Arrange
        String accountId = "acc-nonexistent";
        
        when(service.findById(accountId)).thenReturn(Optional.empty());
        
        // Act & Assert
        mockMvc.perform(delete("/api/accounts/{id}", accountId))
            .andExpect(status().isNotFound());
        
        verify(service).findById(accountId);
        verify(service, never()).save(any());
    }

    @Test
    void delete_withoutAuth_shouldReturn401() throws Exception {
        String accountId = "acc-001";
        
        mockMvc.perform(delete("/api/accounts/{id}", accountId))
            .andExpect(status().isUnauthorized());
        
        verify(service, never()).findById(any());
        verify(service, never()).save(any());
    }
}
