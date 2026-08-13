package com.rishabh.microservices.auth.repository;

import com.rishabh.microservices.auth.entity.Identity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdentityRepository extends JpaRepository<Identity, Long> {

    // Email is the primary lookup key for login and registration checks
    Optional<Identity> findByEmail(String email);
}
