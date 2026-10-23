package com.example.backend.exception;

/** Recurso inexistente: se traduce a HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
