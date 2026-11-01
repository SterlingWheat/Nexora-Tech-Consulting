package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NotificationTemplateUpdateRequest(
        @NotBlank(message = "El asunto es obligatorio.")
        @Size(max = 200, message = "El asunto no puede superar 200 caracteres.")
        String subject,
        @NotBlank(message = "El contenido HTML es obligatorio.")
        @Size(max = 50000, message = "El contenido HTML supera el tamaño permitido.")
        String htmlBody,
        @NotBlank(message = "El contenido de texto es obligatorio.")
        @Size(max = 50000, message = "El contenido de texto supera el tamaño permitido.")
        String textBody,
        @NotNull(message = "Debes indicar si la plantilla está activa.")
        Boolean active) {
}
