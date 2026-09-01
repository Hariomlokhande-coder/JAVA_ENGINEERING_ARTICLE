package com.technicalblog.exception;

/** Thrown when an upload cannot be stored on disk. Mapped to HTTP 500. */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
