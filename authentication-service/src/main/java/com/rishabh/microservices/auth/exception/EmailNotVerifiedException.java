package com.rishabh.microservices.auth.exception;

// Thrown when password setup is attempted before email verification is complete.
public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
