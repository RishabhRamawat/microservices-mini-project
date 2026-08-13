package com.rishabh.microservices.auth.exception;

// Thrown when BCrypt match fails — the supplied OTP does not match the stored hash.
public class OtpInvalidException extends RuntimeException {

    public OtpInvalidException(String message) {
        super(message);
    }
}
