package com.example.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailTestRequest(
        @NotBlank(message = "El destinatario es obligatorio.")
        @Email(message = "El destinatario debe ser un correo válido.")
        String recipient) {
}
