package com.technicalblog.exception;

/** Thrown when a unique field (slug, email, tag) is already taken. Mapped to HTTP 409. */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
