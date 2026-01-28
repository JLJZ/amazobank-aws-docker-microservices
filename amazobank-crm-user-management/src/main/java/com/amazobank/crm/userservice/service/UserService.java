package com.amazobank.crm.userservice.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amazobank.crm.userservice.config.CognitoConfig;
import com.amazobank.crm.userservice.domain.User;
import com.amazobank.crm.userservice.repo.UserRepository;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;

@Service
@Slf4j
public class UserService {
    private final UserRepository repo;
    private final CognitoIdentityProviderClient cognitoClient;
    private final String userPoolId;

    public UserService(UserRepository repo, CognitoIdentityProviderClient cognitoClient, CognitoConfig cognitoConfig) {
        this.repo = repo;
        this.cognitoClient = cognitoClient;
        this.userPoolId = cognitoConfig.userPoolId();
    }

    public Optional<User> findByEmail(String email) {
        return repo.findByEmail(email);
    }

    public List<User> findAll() {
        return repo.findAll();
    }

    public User create(User user, String password) {
        return createUserInCognito(user, password);
    }

    private User createUserInCognito(User user, String password) {
        try {
            var attributes = toAttributeTypes(user);
            
            if (password == null || password.isEmpty()) {
                log.debug("Generating random password for user");
                password = UUID.randomUUID().toString();
            }
            
            var cognitoUsername = user.getEmail();

            // Cognito requires a temporary password for adminCreateUser.
            var userRequest = AdminCreateUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(cognitoUsername)  // Email is set as the username on the pool
                    .temporaryPassword(password)
                    .userAttributes(attributes)
                    .messageAction("SUPPRESS") // Use "SUPPRESS" to avoid sending a welcome email from Cognito
                    .build();
                
            var groupRequest = AdminAddUserToGroupRequest.builder()
                    .groupName(user.getRole().name())
                    .userPoolId(userPoolId)
                    .username(cognitoUsername)
                    .build();

            var createdUser = createUserInGroup(user, userRequest, groupRequest);
                    
            log.atInfo()
                .setMessage("User {} created in Cognito. Role: {}")
                .addArgument(createdUser.getUserId())
                .addArgument(createdUser.getRole())
                .log();
            
            return createdUser;

        } catch (CognitoIdentityProviderException e) {
            // Log the error or rethrow a custom exception
            log.error("Error creating user in Cognito", e.awsErrorDetails().errorMessage());
            // It's important to handle this error appropriately.
            // For now, rethrowing as a RuntimeException.
            throw new RuntimeException("Failed to create user in Cognito", e);
        }
    }
    
    @Transactional
    private User createUserInGroup(User user, AdminCreateUserRequest userRequest, AdminAddUserToGroupRequest groupRequest) {
        var response = cognitoClient.adminCreateUser(userRequest);
        cognitoClient.adminAddUserToGroup(groupRequest);
        var createdUser = response.user();
        String sub = createdUser.attributes().stream()
                .filter(attr -> "sub".equals(attr.name()))
                .map(AttributeType::value)
                .findFirst()
                .orElseThrow(RuntimeException::new);
        user.setUserId(sub);
        return repo.save(user);
    }
    
    @PreAuthorize("hasRole('ROLE_SUPERADMIN', 'ROLE_ADMIN')")
    public Optional<Map.Entry<HttpStatus, String>> deleteUser(String id) {
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        assert authorities.size() == 1;
        var authority = authorities.stream().findFirst().orElseThrow(AssertionError::new);
        var role = User.Role.fromAuthority(authority.getAuthority());
        return repo.deleteById(role, id);
    }

    @Transactional
    @PreAuthorize("hasRole('ROLE_SUPERADMIN', 'ROLE_ADMIN')")
    public Optional<Entry<HttpStatus,String>> updateUser(User user, String password) {
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        assert authorities.size() == 1;
        var authority = authorities.stream().findFirst().orElseThrow(AssertionError::new);
        var role = User.Role.fromAuthority(authority.getAuthority());
        var attributes = toAttributeTypes(user);

        var cognitoUsername = user.getEmail(); // Email is set as the username on the pool

        cognitoClient.adminUpdateUserAttributes(req -> req
                .userPoolId(userPoolId)
                .username(cognitoUsername)  
                .userAttributes(attributes));
        
        if (password != null && !password.isEmpty()) {
            cognitoClient.adminSetUserPassword(req -> req
                .userPoolId(userPoolId)
                .username(cognitoUsername)
                .password(password));
        }

        return repo.update(role, user);
    }
    
    private static Collection<AttributeType> toAttributeTypes(User user) {
        var emailAttr = AttributeType.builder()
                .name("email")
                .value(user.getEmail())
                .build();
        var firstNameAttr = AttributeType.builder()
                .name("given_name")
                .value(user.getFirstName())
                .build();
        var lastNameAttr = AttributeType.builder()
                .name("family_name")
                .value(user.getLastName())
                .build();
        return List.of(emailAttr, firstNameAttr, lastNameAttr);
    }
}
