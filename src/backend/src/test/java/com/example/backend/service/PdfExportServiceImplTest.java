package com.example.backend.service;

import com.example.backend.dto.ReporteTablaDTO;
import com.example.backend.service.impl.PdfExportServiceImpl;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// RF-47 (KAN-58), criterio 1: el PDF generado debe contener el mismo contenido
// (título, encabezados y filas) del reporte, y debe abrir como un PDF válido.
class PdfExportServiceImplTest {

    private final PdfExportServiceImpl exportador = new PdfExportServiceImpl();

    @Test
    void exportar_generaUnPdfValidoConElMismoContenidoDelReporte() throws IOException {
        ReporteTablaDTO reporte = new ReporteTablaDTO(
                "Reporte de voluntarios",
                Map.of(),
                List.of("Nombre", "Activo", "Fecha de registro"),
                List.of(List.of("Ana Pérez", true, LocalDate.of(2026, 1, 15)))
        );

        byte[] contenido = exportador.exportar(reporte);
        assertTrue(contenido.length > 0);

        try (PDDocument documento = Loader.loadPDF(contenido)) {
            assertEquals(1, documento.getNumberOfPages());

            String texto = new PDFTextStripper().getText(documento);
            assertTrue(texto.contains("Reporte de voluntarios"));
            assertTrue(texto.contains("Nombre"));
            assertTrue(texto.contains("Ana Pérez"));
            assertTrue(texto.contains("Sí"));
            assertTrue(texto.contains("2026-01-15"));
        }
    }

    // Bug corregido (QA): un emoji u otro carácter fuera de WinAnsiEncoding abortaba toda la
    // exportación con IllegalArgumentException. Ahora se reemplaza solo ese carácter y el resto
    // del reporte (incluida la fila siguiente) se exporta con normalidad.
    @Test
    void exportar_conEmojiUOtroCaracterNoSoportado_noFallaYSustituyeElCaracter() throws IOException {
        ReporteTablaDTO reporte = new ReporteTablaDTO(
                "Reporte de tareas",
                Map.of(),
                List.of("Título", "Estado"),
                List.of(
                        List.of("Entrega de donaciones 😀", "Pendiente"),
                        List.of("Siguiente tarea", "Finalizada")
                )
        );

        byte[] contenido = exportador.exportar(reporte);
        assertTrue(contenido.length > 0);

        try (PDDocument documento = Loader.loadPDF(contenido)) {
            String texto = new PDFTextStripper().getText(documento);
            assertTrue(texto.contains("Entrega de donaciones ?"));
            assertTrue(texto.contains("Siguiente tarea"));
            assertTrue(texto.contains("Finalizada"));
        }
    }

    @Test
    void exportar_conMuchasFilas_generaVariasPaginas() throws IOException {
        List<List<Object>> filas = new ArrayList<>();
        for (int i = 0; i < 80; i++) {
            filas.add(List.of("Voluntario " + i, i % 2 == 0));
        }
        ReporteTablaDTO reporte = new ReporteTablaDTO("Reporte de voluntarios", Map.of(), List.of("Nombre", "Activo"), filas);

        byte[] contenido = exportador.exportar(reporte);

        try (PDDocument documento = Loader.loadPDF(contenido)) {
            assertTrue(documento.getNumberOfPages() > 1);

            String texto = new PDFTextStripper().getText(documento);
            assertTrue(texto.contains("Voluntario 0"));
            assertTrue(texto.contains("Voluntario 79"));
        }
    }
}
