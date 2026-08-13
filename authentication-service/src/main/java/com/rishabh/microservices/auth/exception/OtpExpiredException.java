package com.rishabh.microservices.auth.exception;

// Thrown when the most recent unverified OTP has passed its expiresAt timestamp.
public class OtpExpiredException extends RuntimeException {

    public OtpExpiredException(String message) {
        super(message);
    }
}
