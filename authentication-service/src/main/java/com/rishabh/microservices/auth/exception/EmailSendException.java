package com.rishabh.microservices.auth.exception;

// Thrown when transactional email delivery via Brevo REST API fails.
public class EmailSendException extends RuntimeException {

    public EmailSendException(String message) {
        super(message);
    }
}
