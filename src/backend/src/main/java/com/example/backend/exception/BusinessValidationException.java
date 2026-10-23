package com.example.backend.exception;

/** Regla de negocio incumplida (p. ej. hora fin anterior a hora inicio): se traduce a HTTP 400. */
public class BusinessValidationException extends RuntimeException {
    public BusinessValidationException(String message) {
        super(message);
    }
}
