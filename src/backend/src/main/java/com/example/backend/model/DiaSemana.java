package com.example.backend.model;

public enum DiaSemana {
    LUNES,
    MARTES,
    MIERCOLES,
    JUEVES,
    VIERNES,
    SABADO,
    DOMINGO;

    /** Número 1 (lunes) a 7 (domingo), como lo guarda la columna disponibilidad_horaria.dia_semana. */
    public short getNumero() {
        return (short) (ordinal() + 1);
    }

    public static DiaSemana desdeNumero(short numero) {
        if (numero < 1 || numero > 7) {
            throw new IllegalArgumentException("Día de la semana fuera de rango (1-7): " + numero);
        }
        return values()[numero - 1];
    }
}
