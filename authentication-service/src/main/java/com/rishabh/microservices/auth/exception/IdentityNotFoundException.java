package com.rishabh.microservices.auth.exception;

// Thrown when no identity exists for the supplied email.
public class IdentityNotFoundException extends RuntimeException {

    public IdentityNotFoundException(String message) {
        super(message);
    }
}
