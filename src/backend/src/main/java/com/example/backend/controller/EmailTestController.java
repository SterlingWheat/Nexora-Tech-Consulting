package com.example.backend.controller;

import com.example.backend.dto.EmailTestRequest;
import com.example.backend.exception.SmtpEmailException;
import com.example.backend.service.SmtpEmailService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/email")
public class EmailTestController {

    private final SmtpEmailService emailService;

    public EmailTestController(SmtpEmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> sendTestEmail(
            @Valid @RequestBody EmailTestRequest request) {
        try {
            emailService.sendEmail(
                    request.recipient(),
                    "Prueba de correo - Nexora",
                    "<p>Este es un correo de prueba enviado por Nexora Tech Consulting.</p>",
                    "Este es un correo de prueba enviado por Nexora Tech Consulting.");
            return ResponseEntity.ok(Map.of("message", "Correo de prueba enviado correctamente."));
        } catch (SmtpEmailException exception) {
            if (exception.getReason() == SmtpEmailException.Reason.CONFIGURATION) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("message", "El servicio de correo no está configurado."));
            }
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "No se pudo enviar el correo. Verifica la configuración SMTP."));
        }
    }
}
