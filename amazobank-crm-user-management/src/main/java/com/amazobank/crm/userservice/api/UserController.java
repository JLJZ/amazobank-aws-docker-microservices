package com.amazobank.crm.userservice.api;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amazobank.crm.userservice.domain.User;
import com.amazobank.crm.userservice.service.UserService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api")
@Slf4j
@Validated
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ROLE_SUPERADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<User> createUser(@Validated @RequestBody UserCreationRequest request, Principal principal, Authentication auth) {
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        
        if (principal != null) {
            log.atInfo().setMessage("Principal: {}; {}")
                .addArgument(principal.getName())
                .addArgument(principal.toString())
                .log();
        } else {
            log.atInfo().setMessage("Principal is null").log();
        }
        
        if (auth == null) {
            log.atInfo().setMessage("Authentication is null").log();
        } else {
            log.atInfo().setMessage("Authentication: {}; {}; {}; {}")
                .addArgument(auth.getName())
                .addArgument(auth.getAuthorities())
                .addArgument(auth.getDetails())
                .addArgument(auth.getPrincipal());
        }

        if (authorities == null || authorities.isEmpty()) {
            log.atError().setMessage("No authorities found in token").log();
            throw new AssertionError("Requests without authorities should have been filtered");
        }

        if (authorities.size() > 1) {
            log.atInfo().setMessage("Multiple roles detected in token").addArgument(authorities).log();
            return ResponseEntity.of(ProblemDetail
                .forStatusAndDetail(HttpStatus.BAD_REQUEST, "Multiple roles detected"))
                .build();
        }
        
        var authority = authorities.iterator().next();

        log.atInfo().setMessage("Received request by {} to create user")
            .addArgument(authority.getAuthority().toString()).log();

        if (authority.getAuthority().equals("ROLE_AGENT")) {
            log.atInfo().setMessage("Agent attempting to create user").addArgument(authority).log();
            return ResponseEntity.of(ProblemDetail
                .forStatusAndDetail(HttpStatus.FORBIDDEN, ""))
                .build();
        }

        var isSuperAdmin = authority.getAuthority().equals("ROLE_SUPERADMIN");
        var role = isSuperAdmin ? User.Role.Admin : User.Role.SuperAdmin;
        if (role == User.Role.Admin && request.getRole() != User.Role.Agent) {
            log.info("Admin attempting to create other admins. Role: ", authority.getAuthority());
            return ResponseEntity.of(ProblemDetail
                .forStatusAndDetail(HttpStatus.FORBIDDEN, "Admins cannot create other admins."))
                .build();
        }

        User newUser = new User();
        newUser.setFirstName(request.getFirstName());
        newUser.setLastName(request.getLastName());
        newUser.setEmail(request.getEmail());
        newUser.setRole(request.getRole());

        User createdUser = userService.create(newUser, request.getPassword());
        log.atInfo().setMessage("User created successfully with ID: {}").addArgument(createdUser.getUserId()).log();
        return ResponseEntity.status(201).body(createdUser);
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ROLE_SUPERADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<List<User>> listUsers() {
        log.atInfo().log("Received request to list all users.");
        List<User> users = userService.findAll();
        log.atInfo().log("Found {} users.", users.size());
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ROLE_SUPERADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable("id") UUID id) {
        log.atInfo().log("Received request to delete user {}");
        
        SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();

        if (authorities == null || authorities.isEmpty()) {
            log.atError().setMessage("No authorities found in token").log();
            throw new AssertionError("Requests without authorities should have been filtered");
        }

        if (authorities.size() > 1) {
            log.atInfo().setMessage("Multiple roles detected in token").addArgument(authorities).log();
            return ResponseEntity.of(ProblemDetail
                .forStatusAndDetail(HttpStatus.BAD_REQUEST, "Multiple roles detected"))
                .build();
        }

        var error = userService.deleteUser(id.toString()).orElse(null);

        if (error == null) {
            log.atInfo().setMessage("User {} deleted successfully").addArgument(id).log();
            return ResponseEntity.ok(Map.of("result", "ok"));

        } else {
            log.atInfo().setMessage(error.getValue()).log();
            return ResponseEntity.status(error.getKey()).body(Map.of(
                            "result", "err",
                            "status", error.getValue()));
        }
    }

    @PatchMapping("/users/{id}")
    @PreAuthorize("hasRole('ROLE_SUPERADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable("id") UUID id, @Validated @RequestBody UserUpdateRequest request) {
        log.atInfo().log("Received request to update user {}");

        SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();

        if (authorities == null || authorities.isEmpty()) {
            log.atError().setMessage("No authorities found in token").log();
            throw new AssertionError("Requests without authorities should have been filtered");
        }

        if (authorities.size() > 1) {
            log.atInfo().setMessage("Multiple roles detected in token").addArgument(authorities).log();
            return ResponseEntity.of(ProblemDetail
                .forStatusAndDetail(HttpStatus.BAD_REQUEST, "Multiple roles detected"))
                .build();
        }
        
        var user = new User();
        user.setUserId(id.toString());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());

        var error = userService.updateUser(user, request.getPassword()).orElse(null);

        if (error == null) {
            log.atInfo().setMessage("User {} updated successfully").addArgument(id).log();
            return ResponseEntity.ok(Map.of("result", "ok"));

        } else {
            log.atInfo().setMessage(error.getValue()).log();
            return ResponseEntity.status(error.getKey()).body(Map.of(
                            "result", "err",
                            "status", error.getValue()));
        }
    }
}
