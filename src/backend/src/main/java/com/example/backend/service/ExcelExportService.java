package com.example.backend.service;

import com.example.backend.dto.ReporteTablaDTO;

// RF-47 (KAN-58): genera el contenido binario .xlsx de un reporte tabular
public interface ExcelExportService {

    byte[] exportar(ReporteTablaDTO reporte);
}
