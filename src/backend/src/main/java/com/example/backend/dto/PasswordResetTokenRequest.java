package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetTokenRequest(
        @NotBlank(message = "El token es obligatorio.")
        @Size(max = 128, message = "El enlace de recuperación no es válido.")
        String token) {
}
