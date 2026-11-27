package com.example.backend.dto;

import java.util.List;

// RF-47 (KAN-58): modelo genérico para exportar un reporte tabular a PDF o Excel.
// Las celdas van como Object (String, Boolean, Integer, LocalDate, etc.) para que el
// exportador de Excel pueda tipar cada columna en lugar de volcar todo como texto.
public record ReporteTablaDTO(String titulo, List<String> encabezados, List<List<Object>> filas) {
}
