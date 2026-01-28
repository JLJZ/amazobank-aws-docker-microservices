package com.amazobank.crm.userservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "`User`")
@Getter @Setter
@ToString @EqualsAndHashCode(callSuper = false)
@NoArgsConstructor @AllArgsConstructor
@Builder
public class User {
    @Id
    @Column(name="UserID", length = 36)
    private String userId;

    @Column(name = "FirstName", nullable = false, length = 50)
    private String firstName;

    @Column(name = "LastName", nullable = false, length = 50)
    private String lastName;

    @Email
    @Column(name = "Email", nullable = false, unique = true, length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "Role", nullable = false, length = 10)
    private Role role;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "UserStatus", nullable = false, length = 10)
    @Builder.Default
    private Status userStatus = Status.Active;

    public enum Role {
        // Order of role determines security level
        // SuperAdmin has highest permissions
        // Lower roles cannot delete/create higher roles
        Agent,
        Admin,
        SuperAdmin;
        public static Role fromAuthority(String authority) {
            return switch (authority) {
                case "ROLE_AGENT" -> Role.Agent;
                case "ROLE_SUPERADMIN" -> Role.SuperAdmin;
                default -> Role.Admin;
            };
        }
    }
    public enum Status { Active, Disabled }
}
