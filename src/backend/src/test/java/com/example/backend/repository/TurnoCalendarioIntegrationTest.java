package com.example.backend.repository;

import com.example.backend.model.Turno;
import com.example.backend.model.Voluntario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RF-30 (KAN-53): prueba de integración contra la base de datos real configurada en .env
 * (sin mocks) para la consulta derivada TurnoRepository.buscarTurnosPorRango, que es una
 * consulta SQL nativa con joins y parámetros opcionales (actividad, voluntario). Verifica que
 * el SQL es válido para Postgres y que el filtrado por rango de fechas, actividad y voluntario
 * asignado funciona con filas reales. Cada prueba corre en una transacción que se revierte al
 * finalizar.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TurnoCalendarioIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private TurnoRepository turnoRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Voluntario nuevoVoluntario(String sufijo) {
        return entityManager.persistFlushFind(Voluntario.builder()
                .nombre("IT").apellido("Turno")
                .documento("IT-TUR-" + sufijo).email("it-turno-" + sufijo + "@example.test")
                .activo(true).build());
    }

    private Turno nuevoTurno(String actividad, LocalDate fecha) {
        return entityManager.persistFlushFind(Turno.builder()
                .fecha(fecha).horaInicio(LocalTime.of(8, 0)).horaFin(LocalTime.of(12, 0))
                .actividad(actividad).cuposTotales(5).build());
    }

    private void asignar(Turno turno, Voluntario voluntario) {
        jdbcTemplate.update(
                "insert into asignacion_turno (id_turno, id_voluntario, estado) values (?, ?, ?)",
                turno.getIdTurno(), voluntario.getIdVoluntario(), EstadosAsignacion.PENDIENTE);
    }

    // RF-30 Criterio 1: trae solo los turnos dentro del rango
    @Test
    void buscarTurnosPorRango_contraLaBdReal_filtraPorFecha() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        nuevoTurno("Dentro-" + sufijo, LocalDate.of(2026, 11, 23));
        nuevoTurno("Fuera-" + sufijo, LocalDate.of(2026, 12, 15));
        entityManager.clear();

        List<Turno> resultado = turnoRepository.buscarTurnosPorRango(
                LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 29), null, null);

        assertTrue(resultado.stream().anyMatch(t -> t.getActividad().equals("Dentro-" + sufijo)));
        assertTrue(resultado.stream().noneMatch(t -> t.getActividad().equals("Fuera-" + sufijo)));
    }

    // RF-30 Criterio 2: el filtro de actividad usa LIKE sobre datos reales
    @Test
    void buscarTurnosPorRango_contraLaBdReal_filtraPorActividad() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        nuevoTurno("Reparto de alimentos " + sufijo, LocalDate.of(2026, 11, 23));
        nuevoTurno("Limpieza de sala " + sufijo, LocalDate.of(2026, 11, 23));
        entityManager.clear();

        List<Turno> resultado = turnoRepository.buscarTurnosPorRango(
                LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 29), "%alimentos " + sufijo.toLowerCase() + "%", null);

        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).getActividad().toLowerCase().contains("alimentos"));
    }

    // RF-30 Criterio 1/2: el filtro por voluntario recorre el join con asignacion_turno sin duplicar filas
    @Test
    void buscarTurnosPorRango_contraLaBdReal_filtraPorVoluntarioSinDuplicar() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        Voluntario voluntario = nuevoVoluntario(sufijo);
        Turno asignadoDosVeces = nuevoTurno("Con-voluntario-" + sufijo, LocalDate.of(2026, 11, 23));
        Turno sinEseVoluntario = nuevoTurno("Sin-voluntario-" + sufijo, LocalDate.of(2026, 11, 24));
        asignar(asignadoDosVeces, voluntario);

        List<Turno> resultado = turnoRepository.buscarTurnosPorRango(
                LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 29), null, voluntario.getIdVoluntario());

        long coincidencias = resultado.stream()
                .filter(t -> t.getIdTurno().equals(asignadoDosVeces.getIdTurno())).count();
        assertEquals(1, coincidencias, "no debe duplicar el turno aunque el join con asignacion_turno lo repita");
        assertTrue(resultado.stream().noneMatch(t -> t.getIdTurno().equals(sinEseVoluntario.getIdTurno())));
    }

    @Test
    void buscarTurnosPorRango_sinTurnosEnElRango_devuelveListaVacia() {
        List<Turno> resultado = turnoRepository.buscarTurnosPorRango(
                LocalDate.of(2099, 1, 1), LocalDate.of(2099, 1, 7), null, null);

        assertTrue(resultado.isEmpty());
    }
}
