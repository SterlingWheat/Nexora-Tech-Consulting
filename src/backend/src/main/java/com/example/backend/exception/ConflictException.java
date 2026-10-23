package com.example.backend.exception;

/** Conflicto con el estado actual (duplicados, etc.): se traduce a HTTP 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
