package com.rishabh.microservices.auth.entity;

import com.rishabh.microservices.auth.enums.Role;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "identities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Identity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Externally exposed identifier; avoids leaking sequential database IDs
    @Column(nullable = false, unique = true, updatable = false)
    private String userId;

    @Column(nullable = false, unique = true)
    private String email;

    // Stores BCrypt hash, never the raw password
    @Column
    private String passwordHash;

    @Column(nullable = false)
    private boolean emailVerified;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // One identity can have multiple OTP verification attempts
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "identity", orphanRemoval = true)
    private List<OtpVerification> otpVerifications = new ArrayList<>();
}
