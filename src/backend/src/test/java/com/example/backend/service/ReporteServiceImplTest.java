package com.example.backend.service;

import com.example.backend.dto.ReporteTablaDTO;
import com.example.backend.model.CategoriaTarea;
import com.example.backend.model.Tarea;
import com.example.backend.model.Turno;
import com.example.backend.model.Voluntario;
import com.example.backend.repository.TareaRepository;
import com.example.backend.service.impl.ReporteServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.when;

// RF-47 (KAN-58): valida que cada reporte arme encabezados y filas con el contenido
// y los tipos correctos a partir de lo que ya exponen los servicios de dominio.
@ExtendWith(MockitoExtension.class)
class ReporteServiceImplTest {

    @Mock
    private VoluntarioService voluntarioService;
    @Mock
    private TareaRepository tareaRepository;
    @Mock
    private TurnoService turnoService;

    @InjectMocks
    private ReporteServiceImpl reporteService;

    @Test
    void reporteVoluntarios_devuelveFilasOrdenadasPorApellidoConTiposCorrectos() {
        Voluntario ana = Voluntario.builder()
                .nombre("Ana").apellido("Zambrano").documento("111").email("ana@test.com")
                .telefono("70000000").carreraProfesion("Ingeniería")
                .activo(true).fechaCreacion(LocalDateTime.of(2026, 1, 10, 9, 0))
                .build();
        Voluntario beto = Voluntario.builder()
                .nombre("Beto").apellido("Alvarez").documento("222").email("beto@test.com")
                .telefono("71111111").carreraProfesion(null)
                .activo(false).fechaCreacion(LocalDateTime.of(2026, 2, 1, 10, 0))
                .build();
        when(voluntarioService.listarVoluntarios()).thenReturn(List.of(ana, beto));

        ReporteTablaDTO reporte = reporteService.reporteVoluntarios();

        assertEquals("Reporte de voluntarios", reporte.titulo());
        assertEquals(8, reporte.encabezados().size());
        assertEquals(2, reporte.filas().size());
        // Alvarez antes que Zambrano
        assertEquals("Alvarez", reporte.filas().get(0).get(1));
        assertEquals(false, reporte.filas().get(0).get(6));
        assertEquals(LocalDate.of(2026, 2, 1), reporte.filas().get(0).get(7));
        assertEquals("Zambrano", reporte.filas().get(1).get(1));
        assertEquals(true, reporte.filas().get(1).get(6));
    }

    @Test
    void reporteTareas_devuelveAvanceComoEnteroYCategoriaComoTexto() {
        CategoriaTarea categoria = CategoriaTarea.builder().idCategoria(1).nombre("Logística").build();
        Tarea tarea = Tarea.builder()
                .titulo("Armar kits").categoria(categoria).estado("En progreso")
                .avance((short) 40).fechaCreacion(LocalDateTime.of(2026, 3, 5, 8, 0))
                .build();
        when(tareaRepository.listarParaReporte()).thenReturn(List.of(tarea));

        ReporteTablaDTO reporte = reporteService.reporteTareas();

        assertEquals("Reporte de tareas", reporte.titulo());
        List<Object> fila = reporte.filas().get(0);
        assertEquals("Armar kits", fila.get(0));
        assertEquals("Logística", fila.get(1));
        assertEquals("En progreso", fila.get(2));
        assertEquals(40, fila.get(3));
        assertInstanceOf(Integer.class, fila.get(3));
        assertEquals(LocalDate.of(2026, 3, 5), fila.get(4));
    }

    @Test
    void reporteTurnos_calculaCuposLibresYOrdenaPorFechaYHora() {
        Turno turno = Turno.builder()
                .fecha(LocalDate.of(2026, 4, 1)).horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(12, 0))
                .actividad("Entrega de alimentos").estado("ACTIVO")
                .cuposTotales(10).cuposOcupados(4)
                .build();
        when(turnoService.listarTurnos()).thenReturn(List.of(turno));

        ReporteTablaDTO reporte = reporteService.reporteTurnos();

        assertEquals("Reporte de turnos", reporte.titulo());
        List<Object> fila = reporte.filas().get(0);
        assertEquals(LocalDate.of(2026, 4, 1), fila.get(0));
        assertEquals(10, fila.get(5));
        assertEquals(4, fila.get(6));
        assertEquals(6, fila.get(7));
    }
}
