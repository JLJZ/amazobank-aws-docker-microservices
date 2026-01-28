package com.amazobank.crm.userservice.repo;

import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.http.HttpStatus;

import com.amazobank.crm.userservice.domain.User;
import com.amazobank.crm.userservice.domain.User.Status;

public interface UserRepository extends JpaRepository<User, String> {
    @Query("SELECT u FROM User u WHERE u.email = ?1 AND u.userStatus = 'Active'")
    Optional<User> findByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.userId = ?1 AND u.userStatus = 'Active'")
    Optional<User> findByUserId(String userId);
    
    default Optional<Map.Entry<HttpStatus, String>> deleteById(User.Role deleterRole, String id) {
        var result = findById(id);

        if (result.isEmpty()) {
            return Optional.of(Map.entry(HttpStatus.NOT_FOUND,"User " + id + " not found"));
        }
        
        var user = result.get();

        if (user.getUserStatus() == Status.Disabled) {
            return Optional.of(Map.entry(HttpStatus.NOT_FOUND,"User " + id + " not found"));
        }
        
        if (deleterRole.compareTo(user.getRole()) <= 0) {
            return Optional.of(Map.entry(HttpStatus.FORBIDDEN, "Not allowed to delete " + id));
        }
        
        user.setUserStatus(Status.Disabled);

        try {
            user = save(user);
        } catch (Exception e) {
            return Optional.of(Map.entry(HttpStatus.FORBIDDEN, "Not allowed to delete " + id));
        }

        return Optional.empty();
    }

    default Optional<Map.Entry<HttpStatus, String>> update(User.Role updaterRole, User newUser) {
        var id = newUser.getUserId();
        var result = findById(id);

        if (result.isEmpty()) {
            return Optional.of(Map.entry(HttpStatus.NOT_FOUND,"User " + id + " not found"));
        }
        
        var user = result.get();

        if (user.getUserStatus() == Status.Disabled) {
            return Optional.of(Map.entry(HttpStatus.NOT_FOUND,"User " + id + " not found"));
        }

        if (updaterRole.compareTo(user.getRole()) <= 0) {
            return Optional.of(Map.entry(HttpStatus.FORBIDDEN, "Not allowed to update " + id));
        }
        if (user.getFirstName().equals(newUser.getFirstName())
                && user.getLastName().equals(newUser.getLastName())
                && user.getEmail().equals(newUser.getEmail())
                && user.getRole().equals(newUser.getRole())) {
            return Optional.empty();  // No changes
        }

        try {
            if (newUser.getFirstName() != null) {
                user.setFirstName(newUser.getFirstName());
            }
            if (newUser.getLastName() != null) {
                user.setLastName(newUser.getLastName());
            }
            if (newUser.getEmail() != null) {
                user.setEmail(newUser.getEmail());
            }
            if (newUser.getRole() != null) {
                user.setRole(newUser.getRole());
            }
            user = save(user);
        } catch (Exception e) {
            return Optional.of(Map.entry(HttpStatus.FORBIDDEN, "Not allowed to update " + id));
        }

        return Optional.empty();
    }
}
