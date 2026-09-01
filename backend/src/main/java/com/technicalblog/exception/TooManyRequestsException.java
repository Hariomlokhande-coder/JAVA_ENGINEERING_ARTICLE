package com.technicalblog.exception;

/** Thrown when a client exceeds the allowed number of failed logins. Mapped to HTTP 429. */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}
