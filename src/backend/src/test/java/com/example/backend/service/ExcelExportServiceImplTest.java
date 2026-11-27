package com.example.backend.service;

import com.example.backend.dto.ReporteTablaDTO;
import com.example.backend.service.impl.ExcelExportServiceImpl;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// RF-47 (KAN-58), criterio 2: el .xlsx generado debe abrir con Apache POI y sus columnas
// deben quedar tipadas (texto, numérico, booleano y fecha), no todo como texto.
class ExcelExportServiceImplTest {

    private final ExcelExportServiceImpl exportador = new ExcelExportServiceImpl();

    @Test
    void exportar_generaUnXlsxValidoConColumnasTipadas() throws IOException {
        ReporteTablaDTO reporte = new ReporteTablaDTO(
                "Reporte de voluntarios",
                Map.of(),
                List.of("Nombre", "Activo", "Cupos", "Fecha de registro"),
                List.of(List.of("Ana Pérez", true, 5, LocalDate.of(2026, 1, 15)))
        );

        byte[] contenido = exportador.exportar(reporte);
        assertTrue(contenido.length > 0);

        try (Workbook libro = WorkbookFactory.create(new ByteArrayInputStream(contenido))) {
            Sheet hoja = libro.getSheetAt(0);

            Row filaEncabezado = hoja.getRow(0);
            assertEquals("Nombre", filaEncabezado.getCell(0).getStringCellValue());
            assertEquals("Activo", filaEncabezado.getCell(1).getStringCellValue());

            Row filaDatos = hoja.getRow(1);
            Cell celdaTexto = filaDatos.getCell(0);
            Cell celdaBooleana = filaDatos.getCell(1);
            Cell celdaNumerica = filaDatos.getCell(2);
            Cell celdaFecha = filaDatos.getCell(3);

            assertEquals(CellType.STRING, celdaTexto.getCellType());
            assertEquals("Ana Pérez", celdaTexto.getStringCellValue());

            assertEquals(CellType.BOOLEAN, celdaBooleana.getCellType());
            assertTrue(celdaBooleana.getBooleanCellValue());

            assertEquals(CellType.NUMERIC, celdaNumerica.getCellType());
            assertEquals(5.0, celdaNumerica.getNumericCellValue());

            assertEquals(CellType.NUMERIC, celdaFecha.getCellType());
            assertEquals(LocalDate.of(2026, 1, 15), celdaFecha.getLocalDateTimeCellValue().toLocalDate());
        }
    }

    @Test
    void exportar_conReporteVacio_generaSoloElEncabezado() throws IOException {
        ReporteTablaDTO reporte = new ReporteTablaDTO("Reporte de tareas", Map.of(), List.of("Título", "Estado"), List.of());

        byte[] contenido = exportador.exportar(reporte);

        try (Workbook libro = WorkbookFactory.create(new ByteArrayInputStream(contenido))) {
            Sheet hoja = libro.getSheetAt(0);
            assertEquals(0, hoja.getLastRowNum());
            assertEquals("Título", hoja.getRow(0).getCell(0).getStringCellValue());
        }
    }
}
