package com.example.carrental.exception;

// TODO: Add error codes or metadata for consumers to handle different scenarios
public class NoAvailableCarException extends RuntimeException {

    public NoAvailableCarException() {
        super();
    }

    public NoAvailableCarException(String message) {
        super(message);
    }

    public NoAvailableCarException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoAvailableCarException(Throwable cause) {
        super(cause);
    }
}
