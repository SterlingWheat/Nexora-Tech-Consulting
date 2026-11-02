package com.example.backend.mail;

import com.example.backend.exception.BusinessValidationException;

import java.util.Locale;

public enum NotificationTemplateType {
    ASIGNACION("Asignación"),
    RECORDATORIO("Recordatorio"),
    CAMBIO_TURNO("Cambio de turno"),
    CANCELACION_ASIGNACION("Cancelación de asignación");

    private final String displayName;

    NotificationTemplateType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static NotificationTemplateType fromPath(String value) {
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new BusinessValidationException("El tipo de plantilla no es válido.");
        }
    }
}
