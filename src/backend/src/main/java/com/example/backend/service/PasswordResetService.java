package com.example.backend.service;

import com.example.backend.dto.PasswordResetConfirmationRequest;
import com.example.backend.dto.PasswordResetResponse;
import com.example.backend.dto.PasswordResetValidationResponse;
import com.example.backend.exception.BusinessValidationException;
import com.example.backend.model.PasswordResetToken;
import com.example.backend.model.Usuario;
import com.example.backend.repository.PasswordResetRateLimitRepository;
import com.example.backend.repository.PasswordResetTokenRepository;
import com.example.backend.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class PasswordResetService {

    public static final String GENERIC_REQUEST_MESSAGE =
            "Si el correo electrónico está registrado, recibirás un enlace para restablecer tu contraseña.";
    private static final String INVALID_TOKEN_MESSAGE =
            "El enlace de recuperación no es válido o expiró.";
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetRateLimitRepository rateLimitRepository;
    private final PasswordResetEmailSender emailSender;
    private final PasswordEncoder passwordEncoder;
    private final String frontendUrl;
    private final String fingerprintSecret;
    private final Duration tokenTtl;
    private final Duration requestCooldown;

    public PasswordResetService(
            UsuarioRepository usuarioRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordResetRateLimitRepository rateLimitRepository,
            PasswordResetEmailSender emailSender,
            PasswordEncoder passwordEncoder,
            @Value("${app.frontend-url:${app.frontend-origin:http://localhost:5173}}") String frontendUrl,
            @Value("${app.password-reset.fingerprint-secret:${app.jwt.secret}}") String fingerprintSecret,
            @Value("${app.password-reset.token-ttl:PT30M}") Duration tokenTtl,
            @Value("${app.password-reset.request-cooldown:PT1M}") Duration requestCooldown) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.rateLimitRepository = rateLimitRepository;
        this.emailSender = emailSender;
        this.passwordEncoder = passwordEncoder;
        this.frontendUrl = frontendUrl;
        this.fingerprintSecret = fingerprintSecret;
        this.tokenTtl = tokenTtl;
        this.requestCooldown = requestCooldown;
        if (tokenTtl.isZero() || tokenTtl.isNegative() || requestCooldown.isNegative()) {
            throw new IllegalArgumentException("La expiración y el intervalo de recuperación deben ser válidos.");
        }
    }

    @Transactional
    public PasswordResetResponse solicitar(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        LocalDateTime now = LocalDateTime.now();
        String fingerprint = fingerprint(normalizedEmail);
        boolean requestAllowed = rateLimitRepository.tryAcquire(
                fingerprint,
                now,
                now.minus(requestCooldown));

        if (requestAllowed) {
            usuarioRepository.findByEmailIgnoreCase(normalizedEmail)
                    .filter(usuario -> Boolean.TRUE.equals(usuario.getActivo()))
                    .ifPresent(usuario -> enviarEnlace(usuario, fingerprint, now));
        }

        return new PasswordResetResponse(GENERIC_REQUEST_MESSAGE);
    }

    @Transactional(readOnly = true)
    public PasswordResetValidationResponse validar(String rawToken) {
        String tokenHash = hash(rawToken);
        boolean valid = tokenRepository
                .existsByTokenHashAndFechaUsoIsNullAndFechaExpiracionAfter(tokenHash, LocalDateTime.now());
        if (!valid) {
            throw new BusinessValidationException(INVALID_TOKEN_MESSAGE);
        }
        return new PasswordResetValidationResponse(true);
    }

    @Transactional
    public PasswordResetResponse restablecer(PasswordResetConfirmationRequest request) {
        LocalDateTime now = LocalDateTime.now();
        PasswordResetToken token = tokenRepository.findByTokenHashForUpdate(hash(request.token()))
                .filter(candidate -> candidate.getFechaUso() == null)
                .filter(candidate -> candidate.getFechaExpiracion().isAfter(now))
                .orElseThrow(() -> new BusinessValidationException(INVALID_TOKEN_MESSAGE));

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessValidationException("Las contraseñas no coinciden.");
        }
        if (!esContrasenaValida(request.newPassword())) {
            throw new BusinessValidationException(
                    "La contraseña debe tener al menos 8 caracteres y no superar el límite seguro de BCrypt.");
        }

        Usuario usuario = token.getUsuario();
        if (usuario == null || !Boolean.TRUE.equals(usuario.getActivo())) {
            throw new BusinessValidationException(INVALID_TOKEN_MESSAGE);
        }

        usuario.setPassword(passwordEncoder.encode(request.newPassword()));
        usuarioRepository.save(usuario);
        token.marcarComoUsado(now);
        tokenRepository.save(token);
        return new PasswordResetResponse("Tu contraseña fue restablecida correctamente.");
    }

    private void enviarEnlace(Usuario usuario, String fingerprint, LocalDateTime now) {
        String rawToken = generarToken();
        tokenRepository.invalidateUnusedTokens(usuario.getIdUsuario(), now);
        PasswordResetToken token = new PasswordResetToken(
                usuario,
                fingerprint,
                hash(rawToken),
                now,
                now.plus(tokenTtl));
        tokenRepository.save(token);

        long expiryMinutes = Math.max(1, tokenTtl.toMinutes());
        String recipient = usuario.getEmail();
        Runnable sendEmail = () -> dispatchEmail(recipient, rawToken, expiryMinutes);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendEmail.run();
                }
            });
        } else {
            sendEmail.run();
        }
    }

    private void dispatchEmail(String recipient, String rawToken, long expiryMinutes) {
        try {
            emailSender.enviar(recipient, rawToken, expiryMinutes, frontendUrl);
        } catch (TaskRejectedException exception) {
            logger.error("Se rechazó el correo de recuperación porque la cola SMTP está llena.");
        }
    }

    private String generarToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("No se pudo procesar el token de recuperación.", exception);
        }
    }

    private String fingerprint(String email) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(fingerprintSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(email.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("No se pudo procesar la solicitud de recuperación.", exception);
        }
    }

    private boolean esContrasenaValida(String password) {
        return password.length() >= 8
                && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
