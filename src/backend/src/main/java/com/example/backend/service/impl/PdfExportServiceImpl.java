package com.example.backend.service.impl;

import com.example.backend.dto.ReporteTablaDTO;
import com.example.backend.service.PdfExportService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

// RF-47 (KAN-58), criterio 1: exporta un reporte tabular a PDF con el mismo contenido
// (título, encabezados y filas) del reporte generado.
@Service
public class PdfExportServiceImpl implements PdfExportService {

    private static final float MARGEN = 40f;
    private static final float ALTO_FILA = 16f;
    private static final float TAMANO_LETRA_TITULO = 14f;
    private static final float TAMANO_LETRA_CELDA = 9f;
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public byte[] exportar(ReporteTablaDTO reporte) {
        try (PDDocument documento = new PDDocument()) {
            PDType1Font fuenteTitulo = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fuenteEncabezado = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fuenteCelda = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            List<String> encabezados = reporte.encabezados();
            List<List<Object>> filas = reporte.filas();

            float[] anchos = calcularAnchosColumna(encabezados.size(), PDRectangle.A4.getWidth() - 2 * MARGEN);

            PDPage pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);
            PDPageContentStream flujo = new PDPageContentStream(documento, pagina);
            float y = PDRectangle.A4.getHeight() - MARGEN;

            float anchoPagina = PDRectangle.A4.getWidth() - 2 * MARGEN;
            y = escribirTexto(flujo, fuenteTitulo, TAMANO_LETRA_TITULO, MARGEN, y, reporte.titulo(), anchoPagina);
            y -= ALTO_FILA;
            y = dibujarFila(flujo, fuenteEncabezado, encabezados.stream().map(Object.class::cast).toList(), anchos, y);

            for (List<Object> fila : filas) {
                if (y < MARGEN + ALTO_FILA) {
                    flujo.close();
                    pagina = new PDPage(PDRectangle.A4);
                    documento.addPage(pagina);
                    flujo = new PDPageContentStream(documento, pagina);
                    y = PDRectangle.A4.getHeight() - MARGEN;
                    y = dibujarFila(flujo, fuenteEncabezado, encabezados.stream().map(Object.class::cast).toList(), anchos, y);
                }
                y = dibujarFila(flujo, fuenteCelda, fila, anchos, y);
            }
            flujo.close();

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            documento.save(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el archivo PDF del reporte", e);
        }
    }

    private float dibujarFila(PDPageContentStream flujo, PDType1Font fuente, List<Object> valores, float[] anchos, float y) throws IOException {
        float x = MARGEN;
        for (int col = 0; col < valores.size(); col++) {
            String texto = formatearValor(valores.get(col));
            escribirTexto(flujo, fuente, TAMANO_LETRA_CELDA, x, y, texto, anchos[col] - 4f);
            x += anchos[col];
        }
        return y - ALTO_FILA;
    }

    private float escribirTexto(PDPageContentStream flujo, PDType1Font fuente, float tamano, float x, float y, String texto, float anchoMaximo) throws IOException {
        flujo.beginText();
        flujo.setFont(fuente, tamano);
        flujo.newLineAtOffset(x, y);
        flujo.showText(recortar(texto, fuente, tamano, anchoMaximo));
        flujo.endText();
        return y;
    }

    private String recortar(String texto, PDType1Font fuente, float tamano, float anchoMaximo) throws IOException {
        if (texto == null) {
            return "";
        }
        String limpio = texto.replace("\r", " ").replace("\n", " ");
        String actual = limpio;
        while (!actual.isEmpty() && fuente.getStringWidth(actual) / 1000 * tamano > anchoMaximo) {
            actual = actual.substring(0, actual.length() - 1);
        }
        return actual;
    }

    private String formatearValor(Object valor) {
        if (valor == null) {
            return "";
        }
        if (valor instanceof java.time.LocalDateTime fechaHora) {
            return fechaHora.format(FORMATO_FECHA_HORA);
        }
        if (valor instanceof Boolean booleano) {
            return booleano ? "Sí" : "No";
        }
        return valor.toString();
    }

    private float[] calcularAnchosColumna(int cantidadColumnas, float anchoDisponible) {
        float[] anchos = new float[cantidadColumnas];
        float anchoIgual = anchoDisponible / cantidadColumnas;
        for (int i = 0; i < anchos.length; i++) {
            anchos[i] = anchoIgual;
        }
        return anchos;
    }
}
