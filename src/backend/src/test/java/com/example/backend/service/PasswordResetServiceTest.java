package com.example.backend.service;

import com.example.backend.dto.PasswordResetConfirmationRequest;
import com.example.backend.dto.LoginRequest;
import com.example.backend.exception.BusinessValidationException;
import com.example.backend.model.PasswordResetToken;
import com.example.backend.model.RolEntity;
import com.example.backend.model.Usuario;
import com.example.backend.repository.PasswordResetRateLimitRepository;
import com.example.backend.repository.PasswordResetTokenRepository;
import com.example.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetServiceTest {

    private UsuarioRepository usuarioRepository;
    private PasswordResetTokenRepository tokenRepository;
    private PasswordResetRateLimitRepository rateLimitRepository;
    private PasswordResetEmailSender emailSender;
    private PasswordEncoder passwordEncoder;
    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        tokenRepository = mock(PasswordResetTokenRepository.class);
        rateLimitRepository = mock(PasswordResetRateLimitRepository.class);
        emailSender = mock(PasswordResetEmailSender.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new PasswordResetService(
                usuarioRepository,
                tokenRepository,
                rateLimitRepository,
                emailSender,
                passwordEncoder,
                "https://app.example.com",
                "test-fingerprint-secret",
                Duration.ofMinutes(30),
                Duration.ofMinutes(1));
    }

    @Test
    void registeredAndUnregisteredEmailsReceiveTheExactSamePublicMessage() {
        when(rateLimitRepository.tryAcquire(anyString(), any(), any())).thenReturn(true);
        when(usuarioRepository.findByEmailIgnoreCase("persona@example.com"))
                .thenReturn(Optional.of(usuario("persona@example.com", true)));
        when(usuarioRepository.findByEmailIgnoreCase("desconocido@example.com"))
                .thenReturn(Optional.empty());

        var registeredResponse = service.solicitar("Persona@Example.com");
        var unknownResponse = service.solicitar("desconocido@example.com");

        assertEquals(PasswordResetService.GENERIC_REQUEST_MESSAGE, registeredResponse.message());
        assertEquals(registeredResponse, unknownResponse);
        verify(emailSender).enviar(anyString(), anyString(), anyLong(), anyString());
    }

    @Test
    void sendsAOneTimeLinkAndPersistsOnlyTheTokenHash() {
        Usuario usuario = usuario("persona@example.com", true);
        when(rateLimitRepository.tryAcquire(anyString(), any(), any())).thenReturn(true);
        when(usuarioRepository.findByEmailIgnoreCase(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(tokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.solicitar(usuario.getEmail());

        var tokenCaptor = org.mockito.ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        PasswordResetToken storedToken = tokenCaptor.getValue();
        var rawTokenCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(emailSender).enviar(
                org.mockito.ArgumentMatchers.eq(usuario.getEmail()),
                rawTokenCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(30L),
                org.mockito.ArgumentMatchers.eq("https://app.example.com"));
        String rawToken = rawTokenCaptor.getValue();
        assertEquals(43, rawToken.length());
        assertNotEquals(rawToken, storedToken.getTokenHash());
        assertEquals(hash(rawToken), storedToken.getTokenHash());
        assertEquals(64, storedToken.getTokenHash().length());
        assertEquals(usuario, storedToken.getUsuario());
        assertTrue(storedToken.getFechaExpiracion().isAfter(storedToken.getFechaCreacion()));
        assertFalse(storedToken.getEmailFingerprint().contains(usuario.getEmail()));
    }

    @Test
    void rateLimitsRepeatedRequestsWithoutRevealingWhetherTheEmailExists() {
        when(rateLimitRepository.tryAcquire(anyString(), any(), any())).thenReturn(false);

        var response = service.solicitar("persona@example.com");

        assertEquals(PasswordResetService.GENERIC_REQUEST_MESSAGE, response.message());
        verify(usuarioRepository, never()).findByEmailIgnoreCase(anyString());
        verify(emailSender, never()).enviar(anyString(), anyString(), anyLong(), anyString());
    }

    @Test
    void fullMailQueueKeepsTheSameGenericResponse() {
        Usuario usuario = usuario("persona@example.com", true);
        when(rateLimitRepository.tryAcquire(anyString(), any(), any())).thenReturn(true);
        when(usuarioRepository.findByEmailIgnoreCase(usuario.getEmail())).thenReturn(Optional.of(usuario));
        org.mockito.Mockito.doThrow(new TaskRejectedException("queue full"))
                .when(emailSender).enviar(anyString(), anyString(), anyLong(), anyString());

        var response = service.solicitar(usuario.getEmail());

        assertEquals(PasswordResetService.GENERIC_REQUEST_MESSAGE, response.message());
        verify(tokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void validTokenChangesPasswordAndCannotBeReused() {
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
        Usuario usuario = usuario("persona@example.com", true);
        usuario.setPassword(bcrypt.encode("anterior-123"));
        PasswordResetService realEncoderService = serviceWithEncoder(bcrypt);
        PasswordResetToken token = token(usuario, "token-seguro", LocalDateTime.now().plusMinutes(5));
        when(tokenRepository.findByTokenHashForUpdate(hash("token-seguro"))).thenReturn(Optional.of(token));
        when(usuarioRepository.findByEmail("persona@example.com")).thenReturn(Optional.of(usuario));

        realEncoderService.restablecer(
                new PasswordResetConfirmationRequest("token-seguro", "nueva-1234", "nueva-1234"));

        assertTrue(bcrypt.matches("nueva-1234", usuario.getPassword()));
        assertFalse(bcrypt.matches("anterior-123", usuario.getPassword()));
        assertTrue(token.getFechaUso() != null);
        verify(usuarioRepository).save(usuario);
        assertThrows(BusinessValidationException.class, () -> realEncoderService.restablecer(
                new PasswordResetConfirmationRequest("token-seguro", "tercera-1234", "tercera-1234")));

        JwtService jwtService = mock(JwtService.class);
        AuthService authService = new AuthService(usuarioRepository, bcrypt, jwtService);
        when(usuarioRepository.findByEmail("persona@example.com")).thenReturn(Optional.of(usuario));
        when(jwtService.generarToken("persona@example.com", "VOLUNTARIO")).thenReturn("jwt");
        authService.iniciarSesion(login("persona@example.com", "nueva-1234"));
        assertThrows(BadCredentialsException.class,
                () -> authService.iniciarSesion(login("persona@example.com", "anterior-123")));
    }

    @Test
    void validatesOnlyUnexpiredUnusedTokens() {
        when(tokenRepository.existsByTokenHashAndFechaUsoIsNullAndFechaExpiracionAfter(
                anyString(), any())).thenReturn(true, false);

        assertTrue(service.validar("token-vigente").valid());
        assertThrows(BusinessValidationException.class, () -> service.validar("token-vencido"));
    }

    @Test
    void rejectsExpiredUsedAndUnknownTokens() {
        Usuario usuario = usuario("persona@example.com", true);
        PasswordResetToken expired = token(usuario, "expirado", LocalDateTime.now().minusSeconds(1));
        when(tokenRepository.findByTokenHashForUpdate(hash("expirado"))).thenReturn(Optional.of(expired));
        when(tokenRepository.findByTokenHashForUpdate(hash("usado")))
                .thenReturn(Optional.of(tokenUsado(usuario, "usado")));
        when(tokenRepository.findByTokenHashForUpdate(hash("inexistente"))).thenReturn(Optional.empty());

        assertThrows(BusinessValidationException.class, () -> service.restablecer(
                new PasswordResetConfirmationRequest("expirado", "nueva-1234", "nueva-1234")));
        assertThrows(BusinessValidationException.class, () -> service.restablecer(
                new PasswordResetConfirmationRequest("usado", "nueva-1234", "nueva-1234")));
        assertThrows(BusinessValidationException.class, () -> service.restablecer(
                new PasswordResetConfirmationRequest("inexistente", "nueva-1234", "nueva-1234")));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void rejectsMismatchedAndTooShortPasswords() {
        Usuario usuario = usuario("persona@example.com", true);
        when(tokenRepository.findByTokenHashForUpdate(hash("valido")))
                .thenReturn(Optional.of(token(usuario, "valido", LocalDateTime.now().plusMinutes(5))));

        assertThrows(BusinessValidationException.class, () -> service.restablecer(
                new PasswordResetConfirmationRequest("valido", "nueva-1234", "otra-1234")));
        assertThrows(BusinessValidationException.class, () -> service.restablecer(
                new PasswordResetConfirmationRequest("valido", "corta", "corta")));
        verify(passwordEncoder, never()).encode(anyString());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    private PasswordResetService serviceWithEncoder(PasswordEncoder encoder) {
        return new PasswordResetService(
                usuarioRepository,
                tokenRepository,
                rateLimitRepository,
                emailSender,
                encoder,
                "https://app.example.com",
                "test-fingerprint-secret",
                Duration.ofMinutes(30),
                Duration.ofMinutes(1));
    }

    private Usuario usuario(String email, boolean active) {
        return Usuario.builder()
                .idUsuario(8)
                .nombre("Persona")
                .email(email)
                .password("hash-anterior")
                .rol(new RolEntity(4, "VOLUNTARIO"))
                .estado(active ? "ACTIVO" : "INACTIVO")
                .build();
    }

    private PasswordResetToken token(Usuario usuario, String rawToken, LocalDateTime expiresAt) {
        LocalDateTime now = LocalDateTime.now();
        return new PasswordResetToken(usuario, "fingerprint", hash(rawToken), now, expiresAt);
    }

    private PasswordResetToken tokenUsado(Usuario usuario, String rawToken) {
        PasswordResetToken token = token(usuario, rawToken, LocalDateTime.now().plusMinutes(5));
        token.marcarComoUsado(LocalDateTime.now().minusSeconds(1));
        return token;
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private LoginRequest login(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

}
