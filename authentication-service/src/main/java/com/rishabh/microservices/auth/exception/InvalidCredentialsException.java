package com.rishabh.microservices.auth.exception;

// Thrown when authentication fails due to incorrect credentials.
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
