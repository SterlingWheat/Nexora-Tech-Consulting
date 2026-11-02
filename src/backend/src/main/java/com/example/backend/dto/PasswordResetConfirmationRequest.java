package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmationRequest(
        @NotBlank(message = "El token es obligatorio.")
        @Size(max = 128, message = "El enlace de recuperación no es válido.")
        String token,
        @NotBlank(message = "La nueva contraseña es obligatoria.")
        @Size(min = 8, max = 256, message = "La contraseña debe tener al menos 8 caracteres.")
        String newPassword,
        @NotBlank(message = "La confirmación de contraseña es obligatoria.")
        @Size(min = 8, max = 256, message = "La contraseña debe tener al menos 8 caracteres.")
        String confirmPassword) {
}
