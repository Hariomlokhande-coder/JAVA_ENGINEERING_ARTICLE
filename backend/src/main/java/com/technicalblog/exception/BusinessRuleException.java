package com.technicalblog.exception;

/** Thrown when an operation is valid syntactically but breaks a domain rule. Mapped to HTTP 409. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
