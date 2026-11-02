package com.example.backend.controller;

import com.example.backend.dto.PasswordResetConfirmationRequest;
import com.example.backend.dto.PasswordResetRequest;
import com.example.backend.dto.PasswordResetResponse;
import com.example.backend.dto.PasswordResetTokenRequest;
import com.example.backend.dto.PasswordResetValidationResponse;
import com.example.backend.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/password-reset")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/request")
    public ResponseEntity<PasswordResetResponse> solicitar(
            @Valid @RequestBody PasswordResetRequest request) {
        return ResponseEntity.ok(passwordResetService.solicitar(request.email()));
    }

    @PostMapping("/validate")
    public ResponseEntity<PasswordResetValidationResponse> validar(
            @Valid @RequestBody PasswordResetTokenRequest request) {
        return ResponseEntity.ok(passwordResetService.validar(request.token()));
    }

    @PostMapping("/confirm")
    public ResponseEntity<PasswordResetResponse> restablecer(
            @Valid @RequestBody PasswordResetConfirmationRequest request) {
        return ResponseEntity.ok(passwordResetService.restablecer(request));
    }
}
