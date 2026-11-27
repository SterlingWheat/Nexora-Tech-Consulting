package com.example.backend.dto;

import java.util.List;
import java.util.Map;

// RF-45/46/47 (KAN-56/58): modelo genérico de un reporte, con un resumen (conteos clave) y una
// tabla de detalle, usado tanto para mostrarlo en pantalla como para exportarlo a PDF/Excel.
// Las celdas van como Object (String, Boolean, Integer, LocalDate, etc.) para conservar el tipo
// real de cada columna, tanto al serializar a JSON como al tipar las columnas del Excel.
public record ReporteTablaDTO(String titulo, Map<String, String> resumen, List<String> encabezados, List<List<Object>> filas) {
}
