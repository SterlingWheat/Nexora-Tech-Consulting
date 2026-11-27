package com.example.backend.service;

import com.example.backend.dto.ReporteTablaDTO;

// RF-47 (KAN-58): genera el contenido binario .pdf de un reporte tabular
public interface PdfExportService {

    byte[] exportar(ReporteTablaDTO reporte);
}
