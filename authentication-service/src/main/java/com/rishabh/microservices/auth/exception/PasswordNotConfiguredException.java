package com.rishabh.microservices.auth.exception;

// Thrown when login is attempted for an identity that has not set a password yet.
public class PasswordNotConfiguredException extends RuntimeException {

    public PasswordNotConfiguredException(String message) {
        super(message);
    }
}
