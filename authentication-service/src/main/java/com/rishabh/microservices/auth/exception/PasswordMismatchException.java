package com.rishabh.microservices.auth.exception;

// Thrown when password and confirmPassword fields do not match.
public class PasswordMismatchException extends RuntimeException {

    public PasswordMismatchException(String message) {
        super(message);
    }
}
