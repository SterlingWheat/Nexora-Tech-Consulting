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

// RF-45/46/47 (KAN-56/58): reportes de voluntarios, tareas y turnos para el Administrador,
// con su resumen y detalle en pantalla, y su exportación a PDF y Excel.
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private static final MediaType TIPO_XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReporteService reporteService;
    private final PdfExportService pdfExportService;
    private final ExcelExportService excelExportService;

    @GetMapping("/{tipo}")
    public ReporteTablaDTO obtenerReporte(@PathVariable String tipo) {
        return reporte(tipo);
    }

    @GetMapping("/{tipo}/pdf")
    public ResponseEntity<byte[]> exportarPdf(@PathVariable String tipo) {
        byte[] contenido = pdfExportService.exportar(reporte(tipo));
        return archivoAdjunto(contenido, MediaType.APPLICATION_PDF, tipo + ".pdf");
    }

    @GetMapping("/{tipo}/excel")
    public ResponseEntity<byte[]> exportarExcel(@PathVariable String tipo) {
        byte[] contenido = excelExportService.exportar(reporte(tipo));
        return archivoAdjunto(contenido, TIPO_XLSX, tipo + ".xlsx");
    }

    private ReporteTablaDTO reporte(String tipo) {
        return switch (tipo) {
            case "voluntarios" -> reporteService.reporteVoluntarios();
            case "tareas" -> reporteService.reporteTareas();
            case "turnos" -> reporteService.reporteTurnos();
            default -> throw new IllegalArgumentException("Tipo de reporte no soportado: " + tipo);
        };
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
