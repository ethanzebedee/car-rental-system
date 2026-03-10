package com.example.carrental.exception;

import java.time.Instant;

public record ErrorResponse(String code, String message, long timestamp) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, Instant.now().toEpochMilli());
    }
}
