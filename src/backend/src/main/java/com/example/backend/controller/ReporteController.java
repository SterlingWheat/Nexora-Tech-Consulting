package com.example.backend.controller;

import com.example.backend.dto.ReporteTablaDTO;
import com.example.backend.service.ExcelExportService;
import com.example.backend.service.PdfExportService;
import com.example.backend.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.function.Supplier;

// RF-47 (KAN-58): exportación de los reportes de voluntarios, tareas y turnos a PDF y Excel
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private static final MediaType TIPO_XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReporteService reporteService;
    private final PdfExportService pdfExportService;
    private final ExcelExportService excelExportService;

    private final Map<String, Supplier<ReporteTablaDTO>> reportesDisponibles = Map.of(
            "voluntarios", this::obtenerVoluntarios,
            "tareas", this::obtenerTareas,
            "turnos", this::obtenerTurnos
    );

    @GetMapping("/{tipo}/pdf")
    public ResponseEntity<byte[]> exportarPdf(@PathVariable String tipo) {
        ReporteTablaDTO reporte = obtenerReporte(tipo);
        byte[] contenido = pdfExportService.exportar(reporte);
        return archivoAdjunto(contenido, MediaType.APPLICATION_PDF, tipo + ".pdf");
    }

    @GetMapping("/{tipo}/excel")
    public ResponseEntity<byte[]> exportarExcel(@PathVariable String tipo) {
        ReporteTablaDTO reporte = obtenerReporte(tipo);
        byte[] contenido = excelExportService.exportar(reporte);
        return archivoAdjunto(contenido, TIPO_XLSX, tipo + ".xlsx");
    }

    private ReporteTablaDTO obtenerReporte(String tipo) {
        Supplier<ReporteTablaDTO> proveedor = reportesDisponibles.get(tipo);
        if (proveedor == null) {
            throw new IllegalArgumentException("Tipo de reporte no soportado: " + tipo);
        }
        return proveedor.get();
    }

    private ReporteTablaDTO obtenerVoluntarios() {
        return reporteService.reporteVoluntarios();
    }

    private ReporteTablaDTO obtenerTareas() {
        return reporteService.reporteTareas();
    }

    private ReporteTablaDTO obtenerTurnos() {
        return reporteService.reporteTurnos();
    }

    private ResponseEntity<byte[]> archivoAdjunto(byte[] contenido, MediaType tipo, String nombreArchivo) {
        ContentDisposition disposicion = ContentDisposition.attachment().filename(nombreArchivo).build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(disposicion);
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(tipo)
                .body(contenido);
    }
}
