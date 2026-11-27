package com.example.backend.dto;

import java.util.List;
import java.util.Map;

// RF-45/46 (KAN-56): modelo genérico de un reporte, con un resumen (conteos clave) y una
// tabla de detalle. Las celdas van como Object (String, Boolean, Integer, LocalDate, etc.)
// para conservar el tipo real de cada columna al serializar a JSON.
public record ReporteTablaDTO(String titulo, Map<String, String> resumen, List<String> encabezados, List<List<Object>> filas) {
}
