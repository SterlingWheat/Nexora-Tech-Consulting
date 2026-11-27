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
    private static final char CARACTER_NO_REPRESENTABLE = '?';

    // Agrupa el content stream de la página actual: al envolver todo el render en un
    // try-with-resources sobre esta clase, un error a mitad de una fila (p. ej. QA: un carácter
    // que la fuente no puede codificar) ya no deja el PDPageContentStream de esa página sin cerrar.
    private static final class PaginaActual implements AutoCloseable {
        private PDPageContentStream flujo;

        void reemplazar(PDPageContentStream nuevo) throws IOException {
            cerrar();
            this.flujo = nuevo;
        }

        void cerrar() throws IOException {
            if (flujo != null) {
                flujo.close();
                flujo = null;
            }
        }

        @Override
        public void close() throws IOException {
            cerrar();
        }
    }

    @Override
    public byte[] exportar(ReporteTablaDTO reporte) {
        try (PDDocument documento = new PDDocument()) {
            PDType1Font fuenteTitulo = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fuenteEncabezado = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fuenteCelda = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            List<String> encabezados = reporte.encabezados();
            List<Object> encabezadosComoObjeto = encabezados.stream().map(Object.class::cast).toList();
            List<List<Object>> filas = reporte.filas();

            float[] anchos = calcularAnchosColumna(encabezados.size(), PDRectangle.A4.getWidth() - 2 * MARGEN);
            float anchoPagina = PDRectangle.A4.getWidth() - 2 * MARGEN;

            try (PaginaActual pagina = new PaginaActual()) {
                pagina.reemplazar(nuevaPagina(documento));
                float y = PDRectangle.A4.getHeight() - MARGEN;

                y = escribirTexto(pagina.flujo, fuenteTitulo, TAMANO_LETRA_TITULO, MARGEN, y, reporte.titulo(), anchoPagina);
                y -= ALTO_FILA;
                y = dibujarFila(pagina.flujo, fuenteEncabezado, encabezadosComoObjeto, anchos, y);

                for (List<Object> fila : filas) {
                    if (y < MARGEN + ALTO_FILA) {
                        pagina.reemplazar(nuevaPagina(documento));
                        y = PDRectangle.A4.getHeight() - MARGEN;
                        y = dibujarFila(pagina.flujo, fuenteEncabezado, encabezadosComoObjeto, anchos, y);
                    }
                    y = dibujarFila(pagina.flujo, fuenteCelda, fila, anchos, y);
                }
                pagina.cerrar();
            }

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            documento.save(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el archivo PDF del reporte", e);
        }
    }

    private PDPageContentStream nuevaPagina(PDDocument documento) throws IOException {
        PDPage pagina = new PDPage(PDRectangle.A4);
        documento.addPage(pagina);
        return new PDPageContentStream(documento, pagina);
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
        String representable = sanearParaFuente(texto, fuente);
        flujo.beginText();
        flujo.setFont(fuente, tamano);
        flujo.newLineAtOffset(x, y);
        flujo.showText(recortar(representable, fuente, tamano, anchoMaximo));
        flujo.endText();
        return y;
    }

    // Bug corregido (QA): un emoji u otro carácter fuera de WinAnsiEncoding hacía que getStringWidth()/
    // showText() lanzaran IllegalArgumentException a mitad de la exportación, abortándola por completo.
    // Primero se intenta el texto completo tal cual (camino feliz, sin costo extra); solo si falla se
    // reemplaza carácter por carácter el que no es representable, en vez de perder todo el reporte.
    private String sanearParaFuente(String texto, PDType1Font fuente) throws IOException {
        if (texto == null || texto.isEmpty()) {
            return texto == null ? "" : texto;
        }
        try {
            fuente.getStringWidth(texto);
            return texto;
        } catch (IllegalArgumentException primerIntentoFallido) {
            int[] codigos = texto.codePoints().toArray();
            StringBuilder saneado = new StringBuilder(codigos.length);
            for (int codigo : codigos) {
                String caracter = new String(Character.toChars(codigo));
                try {
                    fuente.getStringWidth(caracter);
                    saneado.append(caracter);
                } catch (IllegalArgumentException caracterNoSoportado) {
                    saneado.append(CARACTER_NO_REPRESENTABLE);
                }
            }
            return saneado.toString();
        }
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
