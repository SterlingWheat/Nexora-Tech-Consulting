package com.example.backend.service.impl;

import com.example.backend.dto.ReporteTablaDTO;
import com.example.backend.model.Tarea;
import com.example.backend.model.Turno;
import com.example.backend.model.Voluntario;
import com.example.backend.repository.TareaRepository;
import com.example.backend.service.ReporteService;
import com.example.backend.service.TurnoService;
import com.example.backend.service.VoluntarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// RF-45/46/47 (KAN-56/58): construye los reportes de voluntarios, tareas y turnos a partir
// de lo que ya exponen los servicios de dominio, con un resumen de conteos y el detalle,
// usado tanto para mostrarlos en pantalla como para exportarlos a PDF/Excel.
@Service
@RequiredArgsConstructor
public class ReporteServiceImpl implements ReporteService {

    private final VoluntarioService voluntarioService;
    // Riesgo arquitectónico corregido (QA): se usa TareaRepository directamente (con JOIN FETCH,
    // ver listarParaReporte()) en vez de TareaService.listarTareas(), que deja Tarea.categoria como
    // proxy LAZY y dependía implícitamente de que open-in-view estuviera activo para resolverse.
    private final TareaRepository tareaRepository;
    private final TurnoService turnoService;

    @Override
    @Transactional(readOnly = true)
    public ReporteTablaDTO reporteVoluntarios() {
        List<String> encabezados = List.of(
                "Nombre", "Apellido", "Documento", "Correo", "Teléfono", "Carrera/Profesión", "Activo", "Fecha de registro"
        );

        List<Voluntario> voluntarios = new ArrayList<>(voluntarioService.listarVoluntarios());
        voluntarios.sort(Comparator.comparing(Voluntario::getApellido, Comparator.nullsLast(String::compareToIgnoreCase)));

        long activos = voluntarios.stream().filter(v -> Boolean.TRUE.equals(v.getActivo())).count();

        Map<String, String> resumen = new LinkedHashMap<>();
        resumen.put("Total", String.valueOf(voluntarios.size()));
        resumen.put("Activos", String.valueOf(activos));
        resumen.put("Inactivos", String.valueOf(voluntarios.size() - activos));

        List<List<Object>> filas = voluntarios.stream()
                .map(v -> List.<Object>of(
                        valorOTexto(v.getNombre()),
                        valorOTexto(v.getApellido()),
                        valorOTexto(v.getDocumento()),
                        valorOTexto(v.getEmail()),
                        valorOTexto(v.getTelefono()),
                        valorOTexto(v.getCarreraProfesion()),
                        Boolean.TRUE.equals(v.getActivo()),
                        v.getFechaCreacion() == null ? "" : v.getFechaCreacion().toLocalDate()
                ))
                .toList();

        return new ReporteTablaDTO("Reporte de voluntarios", resumen, encabezados, filas);
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteTablaDTO reporteTareas() {
        List<String> encabezados = List.of(
                "Título", "Categoría", "Estado", "Avance (%)", "Fecha de creación"
        );

        List<Tarea> tareas = new ArrayList<>(tareaRepository.listarParaReporte());
        tareas.sort(Comparator.comparing(Tarea::getFechaCreacion, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        Map<String, String> resumen = new LinkedHashMap<>();
        resumen.put("Total", String.valueOf(tareas.size()));
        Map<String, Long> porEstado = new TreeMap<>();
        for (Tarea tarea : tareas) {
            String estado = tarea.getEstado() == null ? "Sin estado" : tarea.getEstado();
            porEstado.merge(estado, 1L, Long::sum);
        }
        porEstado.forEach((estado, cantidad) -> resumen.put(estado, String.valueOf(cantidad)));

        List<List<Object>> filas = tareas.stream()
                .map(t -> List.<Object>of(
                        valorOTexto(t.getTitulo()),
                        t.getCategoria() == null ? "" : valorOTexto(t.getCategoria().getNombre()),
                        valorOTexto(t.getEstado()),
                        (int) (t.getAvance() == null ? 0 : t.getAvance()),
                        t.getFechaCreacion() == null ? "" : t.getFechaCreacion().toLocalDate()
                ))
                .toList();

        return new ReporteTablaDTO("Reporte de tareas", resumen, encabezados, filas);
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteTablaDTO reporteTurnos() {
        List<String> encabezados = List.of(
                "Fecha", "Hora inicio", "Hora fin", "Actividad", "Estado", "Cupos totales", "Cupos ocupados", "Cupos libres"
        );

        List<Turno> turnos = new ArrayList<>(turnoService.listarTurnos());
        turnos.sort(Comparator.comparing(Turno::getFecha, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Turno::getHoraInicio, Comparator.nullsLast(Comparator.naturalOrder())));

        int cuposTotales = turnos.stream().mapToInt(t -> t.getCuposTotales() == null ? 0 : t.getCuposTotales()).sum();
        int cuposOcupados = turnos.stream().mapToInt(t -> t.getCuposOcupados() == null ? 0 : t.getCuposOcupados()).sum();

        Map<String, String> resumen = new LinkedHashMap<>();
        resumen.put("Total turnos", String.valueOf(turnos.size()));
        resumen.put("Cupos totales", String.valueOf(cuposTotales));
        resumen.put("Cupos ocupados", String.valueOf(cuposOcupados));
        resumen.put("Cupos libres", String.valueOf(cuposTotales - cuposOcupados));

        List<List<Object>> filas = turnos.stream()
                .map(t -> List.<Object>of(
                        t.getFecha() == null ? "" : t.getFecha(),
                        t.getHoraInicio() == null ? "" : t.getHoraInicio(),
                        t.getHoraFin() == null ? "" : t.getHoraFin(),
                        valorOTexto(t.getActividad()),
                        valorOTexto(t.getEstado()),
                        t.getCuposTotales() == null ? 0 : t.getCuposTotales(),
                        t.getCuposOcupados() == null ? 0 : t.getCuposOcupados(),
                        t.getCuposLibres()
                ))
                .toList();

        return new ReporteTablaDTO("Reporte de turnos", resumen, encabezados, filas);
    }

    private static String valorOTexto(String valor) {
        return valor == null ? "" : valor;
    }
}
