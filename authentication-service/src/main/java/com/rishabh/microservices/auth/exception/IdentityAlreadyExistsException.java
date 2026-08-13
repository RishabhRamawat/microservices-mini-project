package com.rishabh.microservices.auth.exception;

// Thrown when a registration attempt is made for an email that already has an identity.
public class IdentityAlreadyExistsException extends RuntimeException {

    public IdentityAlreadyExistsException(String message) {
        super(message);
    }
}
