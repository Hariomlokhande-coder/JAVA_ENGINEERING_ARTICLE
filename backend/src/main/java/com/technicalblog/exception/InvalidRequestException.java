package com.technicalblog.exception;

/** Thrown for input that bean validation cannot express. Mapped to HTTP 400. */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
