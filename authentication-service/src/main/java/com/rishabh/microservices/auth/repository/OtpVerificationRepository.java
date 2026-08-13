package com.rishabh.microservices.auth.repository;

import com.rishabh.microservices.auth.entity.Identity;
import com.rishabh.microservices.auth.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    // Returns the newest unverified OTP for an identity.
    // Ordering by createdAt DESC ensures a post-resend OTP is always preferred over any older record.
    Optional<OtpVerification> findTopByIdentityAndVerifiedFalseOrderByCreatedAtDesc(Identity identity);
}

