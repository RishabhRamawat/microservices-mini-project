package com.rishabh.microservices.auth.exception;

// Thrown when a password is already set and a second setup attempt is made.
// Password change is a separate authenticated-user operation, not handled here.
public class PasswordAlreadySetException extends RuntimeException {

    public PasswordAlreadySetException(String message) {
        super(message);
    }
}
