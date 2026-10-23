package com.example.backend.service;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "test-secret-with-at-least-32-characters-long";

    @Test
    void validaTokenFirmadoYRechazaTokenAlterado() {
        JwtService jwtService = new JwtService(SECRET, Duration.ofMinutes(5));
        String token = jwtService.generarToken("persona@example.com", "VOLUNTARIO");

        assertTrue(jwtService.validarToken(token));
        assertFalse(jwtService.validarToken(token.substring(0, token.length() - 1) + "x"));
    }

    @Test
    void rechazaTokenExpirado() throws InterruptedException {
        JwtService jwtService = new JwtService(SECRET, Duration.ofMillis(1));
        String token = jwtService.generarToken("persona@example.com", "VOLUNTARIO");
        Thread.sleep(20);

        assertFalse(jwtService.validarToken(token));
        assertThrows(JwtException.class, () -> jwtService.parsearClaims(token));
    }
}
