package com.example.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NotificationEmailRequest(
        @NotBlank(message = "El destinatario es obligatorio.")
        @Email(message = "El destinatario debe ser un correo válido.")
        @Size(max = 254, message = "El destinatario supera el tamaño permitido.")
        String recipient,
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 150, message = "El nombre supera el tamaño permitido.")
        String name,
        @NotBlank(message = "La actividad es obligatoria.")
        @Size(max = 500, message = "La actividad supera el tamaño permitido.")
        String activity,
        @NotBlank(message = "La fecha es obligatoria.")
        @Size(max = 100, message = "La fecha supera el tamaño permitido.")
        String date) {
}
