package com.example.carrental.exception;

/**
 * Exception thrown when no cars of the requested type are available
 * for the requested time period in the car rental system.
 */
public class NoAvailableCarException extends RuntimeException {

    public NoAvailableCarException() {
        super("No cars available for the requested dates");
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
