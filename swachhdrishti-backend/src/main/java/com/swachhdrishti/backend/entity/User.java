package com.swachhdrishti.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

// Matches the "users" table. Every citizen, worker, and officer is a row here.
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    // PBKDF2 hash of the password ("pbkdf2_sha256$iterations$salt$hash"). Never the password itself.
    @Column(length = 200)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private Role role;

    private Integer cleanCoins = 0;

    private Integer totalReports = 0;

    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Role {
        CITIZEN, WORKER, OFFICER
    }
}
