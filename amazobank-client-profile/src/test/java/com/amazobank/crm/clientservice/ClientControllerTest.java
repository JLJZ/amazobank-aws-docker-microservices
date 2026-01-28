package com.amazobank.crm.clientservice;

import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.amazobank.crm.clientservice.api.ClientController;
import com.amazobank.crm.clientservice.api.dto.CreateClientRequest;
import com.amazobank.crm.clientservice.api.dto.UpdateClientRequest;
import com.amazobank.crm.clientservice.domain.Client;
import com.amazobank.crm.clientservice.domain.ClientStatus;
import com.amazobank.crm.clientservice.domain.Gender;
import com.amazobank.crm.clientservice.security.SecurityConfig;
import com.amazobank.crm.clientservice.service.ClientService;
import com.amazobank.crm.clientservice.service.SqsService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(ClientController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
public class ClientControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private ClientService service;

        @MockitoBean
        private SqsService sqsService;

        @MockitoBean
        private JwtDecoder jwtDecoder;

        @BeforeEach
        void stubEmailNotifications() {
                Mockito.doNothing()
                                .when(sqsService)
                                .sendEmailNotification(Mockito.anyString(), Mockito.anyString());
        }

        /****************************************
         * GET /api/clients Tests
         ****************************************/
        @Test
        @WithMockUser(username = "a1b2c3d4-5678-90ab-cdef-222222222222", roles = { "ADMIN" })
        void getAll_whenAdmin_shouldReturn403() throws Exception {
                mockMvc.perform(get("/api/clients"))
                                .andExpect(status().isForbidden());

                verify(service, never()).findByAgentId(any());
        }

        @Test
        @WithMockUser(username = "a1b2c3d4-5678-90ab-cdef-111111111111", roles = { "AGENT" })
        void getAll_whenClientBelongsToAgent_shouldReturnClient() throws Exception {
                // Arrange
                UUID clientId = UUID.randomUUID();
                UUID agentId = UUID.fromString("a1b2c3d4-5678-90ab-cdef-111111111111");

                Client client = Client.builder()
                                .clientId(clientId)
                                .agentId(agentId) // Client belongs to this agent
                                .firstName("John")
                                .lastName("Smith")
                                .dateOfBirth(LocalDate.of(1985, 3, 15))
                                .gender(Gender.Male)
                                .email("john.smith@example.com")
                                .phoneNumber("+1-555-0101")
                                .address("123 Main Street")
                                .city("New York")
                                .state("NY")
                                .country("USA")
                                .postalCode("10001")
                                .build();

                when(service.findByAgentId(agentId)).thenReturn(List.of(client));

                // Act & Assert
                mockMvc.perform(get("/api/clients"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].clientId").value(clientId.toString()))
                                .andExpect(jsonPath("$[0].firstName").value("John"))
                                .andExpect(jsonPath("$[0].agentId").value(agentId.toString()));

                verify(service).findByAgentId(agentId);
        }

        @Test
        @WithMockUser(username = "f9e8d7c6-1234-5678-90ab-222222222222", roles = { "AGENT" })
        void getOne_whenClientBelongsToDifferentAgent_shouldReturn403() throws Exception {
                // Arrange
                UUID clientId = UUID.randomUUID();
                UUID ownerAgentId = UUID.fromString("a1b2c3d4-5678-90ab-cdef-111111111111");

                Client client = Client.builder()
                                .clientId(clientId)
                                .agentId(ownerAgentId) // Belongs to different agent
                                .firstName("John")
                                .lastName("Smith")
                                .dateOfBirth(LocalDate.of(1985, 3, 15))
                                .gender(Gender.Male)
                                .email("john.smith@example.com")
                                .phoneNumber("+1-555-0101")
                                .address("123 Main Street")
                                .city("New York")
                                .state("NY")
                                .country("USA")
                                .postalCode("10001")
                                .build();

                when(service.findById(clientId)).thenReturn(Optional.of(client));

                // Act & Assert
                mockMvc.perform(get("/api/clients/{id}", clientId))
                                .andExpect(status().isForbidden());

                verify(service, never()).findByAgentId(ownerAgentId);
        }

        @Test
        @WithMockUser(username = "a1b2c3d4-5678-90ab-cdef-111111111111", roles = { "AGENT" })
        void getOne_whenClientNotFound_shouldReturn404() throws Exception {
                // Arrange
                UUID clientId = UUID.randomUUID();

                when(service.findById(clientId)).thenReturn(Optional.empty());

                // Act & Assert
                mockMvc.perform(get("/api/clients/{id}", clientId))
                                .andExpect(status().isNotFound());

                verify(service).findById(clientId);
        }

        @Test
        void getAll_withoutAuth_shouldReturn401() throws Exception {
                mockMvc.perform(get("/api/clients"))
                                .andExpect(status().isUnauthorized());

                verify(service, never()).findByAgentId(any());
        }

        @Test
        void getOne_withoutAuth_shouldReturn401() throws Exception {
                UUID clientId = UUID.randomUUID();

                mockMvc.perform(get("/api/clients/{id}", clientId))
                                .andExpect(status().isUnauthorized());

                verify(service, never()).findByAgentId(any());
        }

        /****************************************
         * POST /api/clients Tests
         ****************************************/
        @Test
        @WithMockUser(username = "a1b2c3d4-5678-90ab-cdef-111111111111", roles = { "AGENT" })
        void createClient_withValidData_shouldReturn201() throws Exception {
                // Arrange (Creating the request object)
                CreateClientRequest req = new CreateClientRequest(
                                "Alice",
                                "Doe",
                                LocalDate.of(1990, 5, 20),
                                "Female",
                                "alice.doe@example.com",
                                "+1555010101", // cannot have dashes
                                "456 Elm Street",
                                "Boston",
                                "MA",
                                "USA",
                                "02101");

                // Mock service duplicate checks
                when(service.findByEmail(req.email())).thenReturn(Optional.empty());
                when(service.findByPhoneNumber(req.phoneNumber())).thenReturn(Optional.empty());

                // Mock save() to return the same client instance
                when(service.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

                // Convert request to JSON
                ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
                String json = mapper.writeValueAsString(req);

                // Act & Assert
                mockMvc.perform(post("/api/clients")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.firstName").value("Alice"))
                                .andExpect(jsonPath("$.lastName").value("Doe"))
                                .andExpect(jsonPath("$.email").value("alice.doe@example.com"))
                                .andExpect(jsonPath("$.agentId").value("a1b2c3d4-5678-90ab-cdef-111111111111"))
                                .andExpect(jsonPath("$.gender").value("Female"));
        }

        @Test
        @WithMockUser(username = "a1b2c3d4-5678-90ab-cdef-111111111111", roles = { "AGENT" })
        void createClient_withDuplicateEmail_shouldReturn400() throws Exception {
                CreateClientRequest req = new CreateClientRequest(
                                "Bob",
                                "Smith",
                                LocalDate.of(1990, 1, 1),
                                "Male",
                                "bob.smith@example.com",
                                "+1555010202",
                                "123 Street",
                                "City",
                                "ST",
                                "USA",
                                "12345");

                // Mock service to indicate email already exists
                when(service.findByEmail(req.email())).thenReturn(Optional.of(new Client()));

                ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
                String json = mapper.writeValueAsString(req);

                mockMvc.perform(post("/api/clients")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Email already exists."));
        }

        @Test
        @WithMockUser(username = "a1b2c3d4-5678-90ab-cdef-111111111111", roles = { "AGENT" })
        void createClient_withDuplicatePhone_shouldReturn400() throws Exception {
                CreateClientRequest req = new CreateClientRequest(
                                "Bob", "Smith", LocalDate.of(1990, 1, 1),
                                "Male", "unique.email@example.com", "+1555010202",
                                "123 Street", "City", "ST", "USA", "12345");

                // Mock email check passes
                when(service.findByEmail(req.email())).thenReturn(Optional.empty());
                // Mock phone number already exists
                when(service.findByPhoneNumber(req.phoneNumber())).thenReturn(Optional.of(new Client()));

                ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
                String json = mapper.writeValueAsString(req);

                mockMvc.perform(post("/api/clients")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Phone number already exists."));
        }

        @Test
        @WithMockUser(username = "a1b2c3d4-5678-90ab-cdef-111111111111", roles = { "AGENT" })
        void createClient_withInvalidPhoneFormat_shouldReturn400() throws Exception {
                CreateClientRequest req = new CreateClientRequest(
                                "Bob", "Smith", LocalDate.of(1990, 1, 1),
                                "Male", "bob.smith@example.com", "123-456-7890", // invalid format
                                "123 Street", "City", "ST", "USA", "12345");

                ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
                String json = mapper.writeValueAsString(req);

                mockMvc.perform(post("/api/clients")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void createClient_withoutAuthentication_shouldReturn401() throws Exception {
                CreateClientRequest req = new CreateClientRequest(
                                "Bob", "Smith", LocalDate.of(1990, 1, 1),
                                "Male", "bob.smith@example.com", "+1555010202",
                                "123 Street", "City", "ST", "USA", "12345");

                ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
                String json = mapper.writeValueAsString(req);

                mockMvc.perform(post("/api/clients")
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isUnauthorized());
        }

        /****************************************
         * PUT /api/clients Tests
         ****************************************/
        @Test
        @WithMockUser(username = "a1b2c3d4-5678-90ab-cdef-111111111111", roles = { "AGENT" })
        void updateClient_successful_shouldReturn200() throws Exception {
                // Arrange
                UUID clientId = UUID.randomUUID();
                UUID agentId = UUID.fromString("a1b2c3d4-5678-90ab-cdef-111111111111");

                // Existing client in DB
                Client existing = Client.builder()
                                .clientId(clientId)
                                .agentId(agentId)
                                .firstName("John")
                                .lastName("Smith")
                                .gender(Gender.Male)
                                .email("john.smith@example.com")
                                .phoneNumber("+1555010101")
                                .build();

                                                // Existing client in DB
                Client updated = Client.builder()
                                .clientId(clientId)
                                .agentId(agentId)
                                .firstName("John")
                                .lastName("Smith")
                                .gender(Gender.Male)
                                .email("jane.doe@example.com")
                                .phoneNumber("+1555010101")
                                .build();

                // Update request
                UpdateClientRequest req = new UpdateClientRequest(
                                null,
                                null,
                                null,
                                null,
                                "jane.doe@example.com",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null);
                when(service.findById(clientId)).thenReturn(Optional.of(existing));
                when(service.findByEmail(req.email())).thenReturn(Optional.empty());
                when(service.findByPhoneNumber(req.phoneNumber())).thenReturn(Optional.empty());
                when(service.update(existing)).thenReturn(updated);

                ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
                String json = mapper.writeValueAsString(req);

                // Act & Assert
                mockMvc.perform(put("/api/clients/{id}", clientId)
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.firstName").value("John"))
                                .andExpect(jsonPath("$.lastName").value("Smith"))
                                .andExpect(jsonPath("$.email").value("jane.doe@example.com"))
                                .andExpect(jsonPath("$.phoneNumber").value("+1555010101"));
        }

        @Test
        @WithMockUser(username = "a1b2c3d4-5678-90ab-cdef-111111111111", roles = { "AGENT" })
        void updateClient_notFound_shouldReturn404() throws Exception {
                UUID clientId = UUID.randomUUID();

                UpdateClientRequest req = new UpdateClientRequest(
                                "Jane", "Doe", LocalDate.of(1990, 5, 20),
                                "Female", "jane.doe@example.com", "+1555010202",
                                "123 New St", "Boston", "MA", "USA", "02101", null);

                when(service.findById(clientId)).thenReturn(Optional.empty());

                ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
                String json = mapper.writeValueAsString(req);

                mockMvc.perform(put("/api/clients/{id}", clientId)
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Client not found"));
        }

        @Test
        @WithMockUser(username = "11111111-2222-3333-4444-555555555555", roles = { "AGENT" })
        void updateClient_forbidden_shouldReturn403() throws Exception {
                UUID clientId = UUID.randomUUID();
                UUID originalAgentId = UUID.fromString("a1b2c3d4-5678-90ab-cdef-111111111111");

                Client existing = Client.builder()
                                .clientId(clientId)
                                .agentId(originalAgentId)
                                .firstName("John")
                                .lastName("Smith")
                                .email("john.smith@example.com")
                                .phoneNumber("+1555010101")
                                .build();

                UpdateClientRequest req = new UpdateClientRequest(
                                "Jane", "Doe", LocalDate.of(1990, 5, 20),
                                "Female", "jane.doe@example.com", "+1555010202",
                                "123 New St", "Boston", "MA", "USA", "02101", null);

                when(service.findById(clientId)).thenReturn(Optional.of(existing));

                ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
                String json = mapper.writeValueAsString(req);

                mockMvc.perform(put("/api/clients/{id}", clientId)
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.message").value("Forbidden"));
        }

        @Test
        @WithMockUser(username = "a1b2c3d4-5678-90ab-cdef-111111111111", roles = { "AGENT" })
        void updateClient_duplicateEmail_shouldReturn400() throws Exception {
                UUID clientId = UUID.randomUUID();
                UUID agentId = UUID.fromString("a1b2c3d4-5678-90ab-cdef-111111111111");

                Client existing = Client.builder()
                                .clientId(clientId)
                                .agentId(agentId)
                                .firstName("John")
                                .lastName("Smith")
                                .email("john.smith@example.com")
                                .phoneNumber("+1555010101")
                                .build();

                UpdateClientRequest req = new UpdateClientRequest(
                                "Jane", "Doe", LocalDate.of(1990, 5, 20),
                                "Female", "existing.email@example.com", "+1555010202",
                                "123 New St", "Boston", "MA", "USA", "02101", null);

                Client other = Client.builder()
                                .clientId(UUID.randomUUID())
                                .agentId(agentId)
                                .email("existing.email@example.com")
                                .phoneNumber("+1555099999")
                                .build();

                when(service.findById(clientId)).thenReturn(Optional.of(existing));
                when(service.findByEmail(req.email())).thenReturn(Optional.of(other));

                ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
                String json = mapper.writeValueAsString(req);

                mockMvc.perform(put("/api/clients/{id}", clientId)
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Email already exists."));
        }

        @Test
        @WithMockUser(username = "a1b2c3d4-5678-90ab-cdef-111111111111", roles = { "AGENT" })
        void updateClient_duplicatePhone_shouldReturn400() throws Exception {
                UUID clientId = UUID.randomUUID();
                UUID agentId = UUID.fromString("a1b2c3d4-5678-90ab-cdef-111111111111");

                Client existing = Client.builder()
                                .clientId(clientId)
                                .agentId(agentId)
                                .firstName("John")
                                .lastName("Smith")
                                .email("john.smith@example.com")
                                .phoneNumber("+1555010101")
                                .build();

                UpdateClientRequest req = new UpdateClientRequest(
                                "Jane", "Doe", LocalDate.of(1990, 5, 20),
                                "Female", "jane.doe@example.com", "+1555099999",
                                "123 New St", "Boston", "MA", "USA", "02101", null);

                Client other = Client.builder()
                                .clientId(UUID.randomUUID())
                                .agentId(agentId)
                                .email("other.email@example.com")
                                .phoneNumber("+1555099999")
                                .build();

                when(service.findById(clientId)).thenReturn(Optional.of(existing));
                when(service.findByEmail(req.email())).thenReturn(Optional.empty());
                when(service.findByPhoneNumber(req.phoneNumber())).thenReturn(Optional.of(other));

                ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
                String json = mapper.writeValueAsString(req);

                mockMvc.perform(put("/api/clients/{id}", clientId)
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Phone number already exists."));
        }

        @Test
        @WithMockUser(username = "a1b2c3d4-5678-90ab-cdef-111111111111", roles = { "AGENT" })
        void updateClient_partialUpdate_shouldUpdateOnlyProvidedFields() throws Exception {
                UUID clientId = UUID.randomUUID();
                UUID agentId = UUID.fromString("a1b2c3d4-5678-90ab-cdef-111111111111");

                Client existing = Client.builder()
                                .clientId(clientId)
                                .agentId(agentId)
                                .firstName("John")
                                .lastName("Smith")
                                .email("john.smith@example.com")
                                .phoneNumber("+1555010101")
                                .address("Old St")
                                .gender(Gender.Male)
                                .build();

                Client updated = Client.builder()
                                .clientId(clientId)
                                .agentId(agentId)
                                .firstName("John")
                                .lastName("Smith")
                                .email("john.smith@example.com")
                                .phoneNumber("+1555010101")
                                .address("123 New St")
                                .gender(Gender.Male)
                                .build();

                // Only updating firstName and address
                UpdateClientRequest req = new UpdateClientRequest(
                                null, // new firstName
                                null, // lastName unchanged
                                null, // dateOfBirth unchanged
                                null, // gender unchanged
                                null, // email unchanged
                                null, // phoneNumber unchanged
                                "123 New St", // address updated
                                null, null, null, null,
                                null);

                when(service.findById(clientId)).thenReturn(Optional.of(existing));
                when(service.findByEmail(any())).thenReturn(Optional.empty());
                when(service.findByPhoneNumber(any())).thenReturn(Optional.empty());
                when(service.update(existing)).thenReturn(updated);

                ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
                String json = mapper.writeValueAsString(req);

                mockMvc.perform(put("/api/clients/{id}", clientId)
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.firstName").value("John"))
                                .andExpect(jsonPath("$.lastName").value("Smith")) // unchanged
                                .andExpect(jsonPath("$.address").value("123 New St"));
        }

        @Test
        @WithMockUser(username = "a1b2c3d4-5678-90ab-cdef-111111111111", roles = { "AGENT" })
        void updateClient_softDeleted_shouldReturn410() throws Exception {
                UUID clientId = UUID.randomUUID();
                UUID agentId = UUID.fromString("a1b2c3d4-5678-90ab-cdef-111111111111");

                // Existing client in DB with DELETED status
                Client existing = Client.builder()
                                .clientId(clientId)
                                .agentId(agentId)
                                .firstName("John")
                                .lastName("Smith")
                                .email("john.smith@example.com")
                                .phoneNumber("+1555010101")
                                .clientStatus(ClientStatus.Deleted) // Soft deleted
                                .build();

                UpdateClientRequest req = new UpdateClientRequest(
                                "Jane", "Doe", LocalDate.of(1990, 5, 20),
                                "Female", "jane.doe@example.com", "+1555010202",
                                "123 New St", "Boston", "MA", "USA", "02101", null);

                when(service.findById(clientId)).thenReturn(Optional.of(existing));

                ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
                String json = mapper.writeValueAsString(req);

                mockMvc.perform(put("/api/clients/{id}", clientId)
                                .contentType("application/json")
                                .content(json))
                                .andExpect(status().isGone())
                                .andExpect(jsonPath("$.message").value("Client has been deleted and cannot be updated"));
        }

        /****************************************
         * DELETE /api/clients Tests
         ****************************************/
        @Test
        @WithMockUser(username = "a1b2c3d4-5678-90ab-cdef-111111111111", roles = { "AGENT" })
        void deleteClient_notFound_shouldReturn404() throws Exception {
                UUID clientId = UUID.randomUUID();
                when(service.findById(clientId)).thenReturn(Optional.empty());

                mockMvc.perform(delete("/api/clients/{id}", clientId))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Client not found"));
        }

        @Test
        @WithMockUser(username = "a1b2c3d4-5678-90ab-cdef-111111111111", roles = { "AGENT" })
        void deleteClient_forbidden_shouldReturn403() throws Exception {
                UUID clientId = UUID.randomUUID();
                UUID otherAgentId = UUID.randomUUID();

                Client client = Client.builder()
                                .clientId(clientId)
                                .agentId(otherAgentId)
                                .build();

                when(service.findById(clientId)).thenReturn(Optional.of(client));

                mockMvc.perform(delete("/api/clients/{id}", clientId))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.message").value("Forbidden"));
        }

        @Test
        @WithMockUser(username = "a1b2c3d4-5678-90ab-cdef-111111111111", roles = { "AGENT" })
        void deleteClient_success_shouldSoftDeleteAndReturn200() throws Exception {
                UUID clientId = UUID.randomUUID();
                UUID agentId = UUID.fromString("a1b2c3d4-5678-90ab-cdef-111111111111");

                Client client = Client.builder()
                                .clientId(clientId)
                                .agentId(agentId)
                                .clientStatus(ClientStatus.Active)
                                .build();

        when(service.findById(clientId)).thenReturn(Optional.of(client));
        Mockito.doNothing().when(service).softDelete(client);

                mockMvc.perform(delete("/api/clients/{id}", clientId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Client deleted successfully"));

        // Capture the saved client for verification 
        verify(service).softDelete(client);
        }
}
