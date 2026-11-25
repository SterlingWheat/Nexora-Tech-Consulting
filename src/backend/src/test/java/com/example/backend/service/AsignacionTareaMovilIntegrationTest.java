package com.example.backend.service;

import com.example.backend.model.AsignacionTarea;
import com.example.backend.model.CategoriaTarea;
import com.example.backend.model.Tarea;
import com.example.backend.model.Voluntario;
import com.example.backend.repository.AsignacionTareaRepository;
import com.example.backend.repository.CategoriaTareaRepository;
import com.example.backend.repository.TareaRepository;
import com.example.backend.repository.VoluntarioRepository;
import com.example.backend.service.impl.AsignacionTareaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * RF-38 (KAN-35): prueba de integración contra la base de datos real configurada en .env
 * (sin mocks) para la consulta móvil de tareas del voluntario. Verifica que la consulta
 * derivada findByVoluntarioIdVoluntario trae las tareas reales persistidas y que el
 * ordenamiento por fecha de asignación (agregado para RF-38 Criterio 1) funciona contra
 * datos reales de Postgres, no solo contra fixtures en memoria. Cada prueba corre en una
 * transacción que se revierte al finalizar.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AsignacionTareaMovilIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private AsignacionTareaRepository asignacionTareaRepository;
    @Autowired
    private TareaRepository tareaRepository;
    @Autowired
    private VoluntarioRepository voluntarioRepository;
    @Autowired
    private CategoriaTareaRepository categoriaTareaRepository;

    private AsignacionTareaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AsignacionTareaServiceImpl(
                asignacionTareaRepository, tareaRepository, voluntarioRepository, null, null);
    }

    private Voluntario nuevoVoluntario(String sufijo) {
        return entityManager.persistFlushFind(Voluntario.builder()
                .nombre("IT").apellido("Movil")
                .documento("IT-MOV-" + sufijo).email("it-movil-" + sufijo + "@example.test")
                .activo(true).build());
    }

    private CategoriaTarea nuevaCategoria(String sufijo) {
        return entityManager.persistFlushFind(CategoriaTarea.builder().nombre("Categoria-" + sufijo).build());
    }

    private Tarea nuevaTarea(String titulo, CategoriaTarea categoria) {
        return entityManager.persistFlushFind(Tarea.builder().titulo(titulo).categoria(categoria).build());
    }

    private AsignacionTarea nuevaAsignacion(Tarea tarea, Voluntario voluntario, LocalDateTime fechaAsignacion) {
        AsignacionTarea asignacion = AsignacionTarea.builder()
                .tarea(tarea).voluntario(voluntario)
                .estadoRespuesta("Pendiente")
                .fechaAsignacion(fechaAsignacion)
                .build();
        return entityManager.persistFlushFind(asignacion);
    }

    // RF-38 Criterio 1: la app móvil obtiene, contra la BD real, titulo/descripcion/estado/fecha
    @Test
    void obtenerAsignacionesDelVoluntario_contraLaBdReal_devuelveLosDatosDeLaTarea() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        CategoriaTarea categoria = nuevaCategoria(sufijo);
        Voluntario voluntario = nuevoVoluntario(sufijo);
        Tarea tarea = nuevaTarea("Limpieza-" + sufijo, categoria);
        nuevaAsignacion(tarea, voluntario, LocalDateTime.of(2026, 11, 20, 9, 0));
        entityManager.clear();

        List<com.example.backend.dto.AsignacionTareaMovilDTO> resultado =
                service.obtenerAsignacionesDelVoluntario(voluntario.getIdVoluntario());

        assertEquals(1, resultado.size());
        assertEquals("Limpieza-" + sufijo, resultado.get(0).titulo());
        assertEquals(LocalDateTime.of(2026, 11, 20, 9, 0), resultado.get(0).fechaAsignacion());
    }

    // RF-38 Criterio 1: el orden cronológico (agregado en esta tarea) se cumple con filas reales de Postgres
    @Test
    void obtenerAsignacionesDelVoluntario_contraLaBdReal_lasOrdenaPorFechaDeAsignacion() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        CategoriaTarea categoria = nuevaCategoria(sufijo);
        Voluntario voluntario = nuevoVoluntario(sufijo);
        Tarea tareaDiciembre = nuevaTarea("Diciembre-" + sufijo, categoria);
        Tarea tareaNoviembre = nuevaTarea("Noviembre-" + sufijo, categoria);

        // Se insertan en orden inverso al esperado para probar que el servicio reordena, no la BD
        nuevaAsignacion(tareaDiciembre, voluntario, LocalDateTime.of(2026, 12, 1, 0, 0));
        nuevaAsignacion(tareaNoviembre, voluntario, LocalDateTime.of(2026, 11, 1, 0, 0));
        entityManager.clear();

        List<com.example.backend.dto.AsignacionTareaMovilDTO> resultado =
                service.obtenerAsignacionesDelVoluntario(voluntario.getIdVoluntario());

        assertEquals(List.of("Noviembre-" + sufijo, "Diciembre-" + sufijo),
                resultado.stream().map(com.example.backend.dto.AsignacionTareaMovilDTO::titulo).toList());
    }

    @Test
    void obtenerAsignacionesDelVoluntario_sinAsignaciones_devuelveListaVacia() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        Voluntario voluntario = nuevoVoluntario(sufijo);
        entityManager.clear();

        List<com.example.backend.dto.AsignacionTareaMovilDTO> resultado =
                service.obtenerAsignacionesDelVoluntario(voluntario.getIdVoluntario());

        assertEquals(0, resultado.size());
    }
}
