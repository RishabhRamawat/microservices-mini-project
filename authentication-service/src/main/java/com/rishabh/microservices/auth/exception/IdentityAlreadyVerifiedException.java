package com.rishabh.microservices.auth.exception;

// Thrown when verification or resend is attempted on an already email-verified identity.
public class IdentityAlreadyVerifiedException extends RuntimeException {

    public IdentityAlreadyVerifiedException(String message) {
        super(message);
    }
}
