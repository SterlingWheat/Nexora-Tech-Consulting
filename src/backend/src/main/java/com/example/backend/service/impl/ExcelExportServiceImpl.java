package com.example.backend.service.impl;

import com.example.backend.dto.ReporteTablaDTO;
import com.example.backend.service.ExcelExportService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

// RF-47 (KAN-58), criterio 2: exporta un reporte tabular a .xlsx con columnas tipadas
// (texto, número, booleano y fecha), no todo volcado como texto.
@Service
public class ExcelExportServiceImpl implements ExcelExportService {

    @Override
    public byte[] exportar(ReporteTablaDTO reporte) {
        try (XSSFWorkbook libro = new XSSFWorkbook()) {
            Sheet hoja = libro.createSheet(limitarNombreHoja(reporte.titulo()));

            CellStyle estiloEncabezado = crearEstiloEncabezado(libro);
            CellStyle estiloFecha = libro.createCellStyle();
            estiloFecha.setDataFormat(libro.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd"));

            Row filaEncabezado = hoja.createRow(0);
            List<String> encabezados = reporte.encabezados();
            for (int col = 0; col < encabezados.size(); col++) {
                Cell celda = filaEncabezado.createCell(col);
                celda.setCellValue(encabezados.get(col));
                celda.setCellStyle(estiloEncabezado);
            }

            List<List<Object>> filas = reporte.filas();
            for (int fila = 0; fila < filas.size(); fila++) {
                Row filaExcel = hoja.createRow(fila + 1);
                List<Object> valores = filas.get(fila);
                for (int col = 0; col < valores.size(); col++) {
                    Cell celda = filaExcel.createCell(col);
                    escribirValor(celda, valores.get(col), estiloFecha);
                }
            }

            for (int col = 0; col < encabezados.size(); col++) {
                hoja.autoSizeColumn(col);
            }

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el archivo Excel del reporte", e);
        }
    }

    private void escribirValor(Cell celda, Object valor, CellStyle estiloFecha) {
        if (valor == null) {
            celda.setBlank();
        } else if (valor instanceof Boolean booleano) {
            celda.setCellValue(booleano);
        } else if (valor instanceof Integer || valor instanceof Long || valor instanceof Short) {
            celda.setCellValue(((Number) valor).doubleValue());
        } else if (valor instanceof Double || valor instanceof Float) {
            celda.setCellValue(((Number) valor).doubleValue());
        } else if (valor instanceof LocalDate fecha) {
            celda.setCellValue(fecha);
            celda.setCellStyle(estiloFecha);
        } else if (valor instanceof LocalTime hora) {
            celda.setCellValue(hora.format(DateTimeFormatter.ofPattern("HH:mm")));
        } else {
            celda.setCellValue(valor.toString());
        }
    }

    private CellStyle crearEstiloEncabezado(Workbook libro) {
        CellStyle estilo = libro.createCellStyle();
        Font fuente = libro.createFont();
        fuente.setBold(true);
        estilo.setFont(fuente);
        return estilo;
    }

    private String limitarNombreHoja(String titulo) {
        String nombre = titulo == null ? "Reporte" : titulo;
        return nombre.length() > 31 ? nombre.substring(0, 31) : nombre;
    }
}
