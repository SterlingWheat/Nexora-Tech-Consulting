package com.example.backend.repository;

/**
 * Estados de asignacion_turno que liberan el cupo y el horario del voluntario.
 * Es la única definición: la usan el conteo de cupos (KAN-21) y la detección de cruces (KAN-22).
 */
public final class EstadosAsignacion {

    public static final String CANCELADA = "Cancelada";
    public static final String RECHAZADA = "Rechazada";

    /** Fragmento para usar dentro de un NOT IN (...) en consultas nativas. */
    public static final String NO_VIGENTES_SQL = "'" + CANCELADA + "', '" + RECHAZADA + "'";

    private EstadosAsignacion() {
    }
}
