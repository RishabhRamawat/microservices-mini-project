package com.rishabh.microservices.auth.exception;

// Thrown when no active (unverified) OTP record exists for the identity.
public class OtpNotFoundException extends RuntimeException {

    public OtpNotFoundException(String message) {
        super(message);
    }
}
