package com.example.backend.repository;

import com.example.backend.model.CategoriaTarea;
import com.example.backend.model.Tarea;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * QA (KAN-56/58): prueba contra la base de datos real que TareaRepository.listarParaReporte()
 * trae Tarea.categoria ya resuelta (JOIN FETCH), sin depender de que open-in-view esté activo.
 * Se llama a entityManager.clear() para cerrar la sesión de persistencia antes de leer la
 * categoría, simulando justamente el escenario (open-in-view desactivado) que antes rompía
 * los reportes de tareas con LazyInitializationException.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TareaReporteIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private TareaRepository tareaRepository;

    @Test
    void listarParaReporte_traeLaCategoriaSinNecesitarSesionAbierta() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        CategoriaTarea categoria = entityManager.persistFlushFind(
                CategoriaTarea.builder().nombre("Logística-" + sufijo).build());
        entityManager.persistFlushFind(Tarea.builder()
                .titulo("Armar kits-" + sufijo).categoria(categoria).estado("Pendiente").avance((short) 0)
                .build());

        List<Tarea> tareas = tareaRepository.listarParaReporte();

        // Se cierra la sesión de persistencia: si la categoría no estuviera ya cargada por el
        // JOIN FETCH, acceder a getNombre() lanzaría LazyInitializationException aquí mismo.
        entityManager.clear();

        boolean encontrada = tareas.stream()
                .filter(t -> t.getTitulo().equals("Armar kits-" + sufijo))
                .anyMatch(t -> ("Logística-" + sufijo).equals(t.getCategoria().getNombre()));

        assertTrue(encontrada, "La tarea creada debe aparecer con su categoría ya resuelta");
    }
}
