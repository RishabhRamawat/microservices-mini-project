package com.rishabh.microservices.auth.enums;

/**
 * Defines the authorization scope for an identity.
 * Stored as a string in the database for readability and extensibility.
 */
public enum Role {
    USER,
    ADMIN
}
