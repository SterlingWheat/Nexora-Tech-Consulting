package com.example.backend.controller;

import com.example.backend.dto.ReporteTablaDTO;
import com.example.backend.exception.GlobalExceptionHandler;
import com.example.backend.service.ExcelExportService;
import com.example.backend.service.PdfExportService;
import com.example.backend.service.ReporteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// RF-47 (KAN-58): valida el contrato HTTP de los endpoints de exportación
// (tipo de contenido, nombre de archivo adjunto y tipo de reporte inválido).
@ExtendWith(MockitoExtension.class)
class ReporteControllerTest {

    @Mock
    private ReporteService reporteService;
    @Mock
    private PdfExportService pdfExportService;
    @Mock
    private ExcelExportService excelExportService;

    @InjectMocks
    private ReporteController reporteController;

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        mockMvc = MockMvcBuilders.standaloneSetup(reporteController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void exportarPdf_devuelveElPdfConLosHeadersCorrectos() throws Exception {
        when(reporteService.reporteVoluntarios()).thenReturn(new ReporteTablaDTO("Reporte de voluntarios", List.of(), List.of()));
        when(pdfExportService.exportar(any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/reportes/voluntarios/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"voluntarios.pdf\""));
    }

    @Test
    void exportarExcel_devuelveElXlsxConLosHeadersCorrectos() throws Exception {
        when(reporteService.reporteTurnos()).thenReturn(new ReporteTablaDTO("Reporte de turnos", List.of(), List.of()));
        when(excelExportService.exportar(any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/reportes/turnos/excel"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"turnos.xlsx\""));
    }

    @Test
    void exportar_conTipoDeReporteInvalido_devuelve400() throws Exception {
        mockMvc.perform(get("/api/reportes/inexistente/pdf"))
                .andExpect(status().isBadRequest());
    }
}
