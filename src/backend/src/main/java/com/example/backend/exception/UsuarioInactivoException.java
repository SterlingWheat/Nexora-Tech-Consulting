package com.example.backend.exception;

/** 
 * Excepción lanzada cuando un usuario intenta iniciar sesión pero su cuenta está desactivada. 
 * Se traduce a HTTP 403 (Forbidden). 
 */
public class UsuarioInactivoException extends RuntimeException {
    
    public UsuarioInactivoException(String message) {
        super(message);
    }
}