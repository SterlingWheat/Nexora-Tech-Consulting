package com.example.backend.repository;

/**
 * Estados de asignacion_turno que liberan o mantienen el cupo y el horario del voluntario.
 * Es la única definición centralizada: la usan el conteo de cupos, la detección de cruces y el flujo de confirmación.
 */
public final class EstadosAsignacion {

    public static final String PENDIENTE = "Pendiente";
    public static final String CONFIRMADA = "Confirmada";
    public static final String CANCELADA = "Cancelada";
    public static final String RECHAZADA = "Rechazada";

    /** Fragmento para usar dentro de un NOT IN (...) en consultas nativas. */
    public static final String NO_VIGENTES_SQL = "'" + CANCELADA + "', '" + RECHAZADA + "'";

    private EstadosAsignacion() {
    }
}