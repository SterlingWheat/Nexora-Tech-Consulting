package com.example.backend.repository;

import com.example.backend.model.DiaSemana;
import com.example.backend.model.Disponibilidad;
import com.example.backend.model.Habilidad;
import com.example.backend.model.Voluntario;
import com.example.backend.service.DisponibilidadService;
import com.example.backend.service.impl.DisponibilidadServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RF-23 (KAN-30): pruebas de integración contra la base de datos real configurada en .env
 * (sin mocks). Verifica que el mapeo JPA de Habilidad/Voluntario y la consulta derivada
 * findByVoluntarioHabilidadesIdHabilidad generan SQL válido contra Postgres, y que la
 * expansión del calendario funciona con datos realmente persistidos. Cada prueba corre en
 * una transacción que se revierte al finalizar, así que no deja datos en la base real.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class HabilidadDisponibilidadIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private HabilidadRepository habilidadRepository;
    @Autowired
    private VoluntarioRepository voluntarioRepository;
    @Autowired
    private DisponibilidadRepository disponibilidadRepository;

    private DisponibilidadService disponibilidadService;

    @BeforeEach
    void setUp() {
        disponibilidadService = new DisponibilidadServiceImpl(disponibilidadRepository, voluntarioRepository);
    }

    private Voluntario nuevoVoluntario(String sufijo) {
        return entityManager.persistFlushFind(Voluntario.builder()
                .nombre("IT").apellido("Voluntario")
                .documento("IT-DOC-" + sufijo)
                .email("it-" + sufijo + "@example.test")
                .activo(true)
                .build());
    }

    @Test
    void habilidadRepository_existsByNombreIgnoreCase_esInsensibleAMayusculasContraLaBdReal() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        habilidadRepository.save(Habilidad.builder().nombre("Cocina-" + sufijo).build());
        entityManager.flush();

        assertTrue(habilidadRepository.existsByNombreIgnoreCase("cocina-" + sufijo));
        assertFalse(habilidadRepository.existsByNombreIgnoreCase("no-existe-" + sufijo));
    }

    @Test
    void voluntarioHabilidad_sePersisteEnLaTablaIntermediaYSeConsultaAlRecargar() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        Habilidad cocina = habilidadRepository.save(Habilidad.builder().nombre("Cocina-" + sufijo).build());
        Voluntario voluntario = nuevoVoluntario(sufijo);

        voluntario.setHabilidades(new HashSet<>(Set.of(cocina)));
        voluntarioRepository.saveAndFlush(voluntario);
        entityManager.clear();

        Voluntario recargado = voluntarioRepository.findById(voluntario.getIdVoluntario()).orElseThrow();
        assertEquals(1, recargado.getHabilidades().size());
        assertEquals(cocina.getIdHabilidad(), recargado.getHabilidades().iterator().next().getIdHabilidad());
    }

    // RF-23 Criterio 2: consulta derivada que recorre voluntario -> habilidades (ManyToMany) -> disponibilidad
    @Test
    void disponibilidadRepository_findByVoluntarioHabilidadesIdHabilidad_soloDevuelveFranjasDeEsaHabilidad() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        Habilidad logistica = habilidadRepository.save(Habilidad.builder().nombre("Logistica-" + sufijo).build());

        Voluntario conHabilidad = nuevoVoluntario(sufijo + "-a");
        conHabilidad.setHabilidades(new HashSet<>(Set.of(logistica)));
        voluntarioRepository.saveAndFlush(conHabilidad);

        Voluntario sinHabilidad = nuevoVoluntario(sufijo + "-b");

        disponibilidadRepository.save(Disponibilidad.builder().voluntario(conHabilidad)
                .diaSemana(DiaSemana.LUNES).horaInicio(LocalTime.of(8, 0)).horaFin(LocalTime.of(12, 0)).build());
        disponibilidadRepository.save(Disponibilidad.builder().voluntario(sinHabilidad)
                .diaSemana(DiaSemana.LUNES).horaInicio(LocalTime.of(8, 0)).horaFin(LocalTime.of(12, 0)).build());
        entityManager.flush();
        entityManager.clear();

        List<Disponibilidad> resultado = disponibilidadRepository.findByVoluntarioHabilidadesIdHabilidad(logistica.getIdHabilidad());

        assertEquals(1, resultado.size());
        assertEquals(conHabilidad.getIdVoluntario(), resultado.get(0).getVoluntario().getIdVoluntario());
    }

    // RF-23 Criterio 1: la lógica de expansión semana->fecha funciona con datos reales de Postgres
    @Test
    void obtenerCalendario_contraLaBdReal_expandeLaFranjaSemanalEnLaFechaCorrecta() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        Voluntario voluntario = nuevoVoluntario(sufijo);
        disponibilidadRepository.save(Disponibilidad.builder().voluntario(voluntario)
                .diaSemana(DiaSemana.LUNES).horaInicio(LocalTime.of(8, 0)).horaFin(LocalTime.of(12, 0)).build());
        entityManager.flush();
        entityManager.clear();

        // 2026-11-23 es lunes
        var resultado = disponibilidadService.obtenerCalendario(
                LocalDate.of(2026, 11, 23), LocalDate.of(2026, 11, 29), voluntario.getIdVoluntario(), null);

        assertEquals(1, resultado.size());
        assertEquals(LocalDate.of(2026, 11, 23), resultado.get(0).fecha());
        assertEquals(voluntario.getIdVoluntario(), resultado.get(0).idVoluntario());
    }

    @Test
    void obtenerCalendario_voluntarioInactivo_noApareceEnElCalendarioGeneral() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        Voluntario inactivo = entityManager.persistFlushFind(Voluntario.builder()
                .nombre("IT").apellido("Inactivo")
                .documento("IT-DOC-" + sufijo).email("it-" + sufijo + "@example.test")
                .activo(false).build());
        disponibilidadRepository.save(Disponibilidad.builder().voluntario(inactivo)
                .diaSemana(DiaSemana.MARTES).horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(10, 0)).build());
        entityManager.flush();
        entityManager.clear();

        var resultado = disponibilidadService.obtenerCalendario(
                LocalDate.of(2026, 11, 23), LocalDate.of(2026, 11, 29), inactivo.getIdVoluntario(), null);

        // El propio filtro por idVoluntario no excluye inactivos a nivel de query, pero el servicio sí los filtra
        assertTrue(resultado.isEmpty());
    }
}
