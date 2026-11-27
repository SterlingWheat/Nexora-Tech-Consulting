package com.example.backend.controller;

import com.example.backend.dto.ReporteTablaDTO;
import com.example.backend.exception.GlobalExceptionHandler;
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
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// RF-45/46 (KAN-56): valida el contrato HTTP del endpoint de reportes (JSON con resumen
// y detalle) y el tipo de reporte inválido.
@ExtendWith(MockitoExtension.class)
class ReporteControllerTest {

    @Mock
    private ReporteService reporteService;

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
    void obtenerReporte_voluntarios_devuelveElResumenYLasFilas() throws Exception {
        ReporteTablaDTO reporte = new ReporteTablaDTO(
                "Reporte de voluntarios",
                Map.of("Total", "2", "Activos", "1"),
                List.of("Nombre", "Activo"),
                List.of(List.of("Ana", true))
        );
        when(reporteService.reporteVoluntarios()).thenReturn(reporte);

        mockMvc.perform(get("/api/reportes/voluntarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Reporte de voluntarios"))
                .andExpect(jsonPath("$.resumen.Total").value("2"))
                .andExpect(jsonPath("$.encabezados[0]").value("Nombre"))
                .andExpect(jsonPath("$.filas[0][0]").value("Ana"));
    }

    @Test
    void obtenerReporte_conTipoInvalido_devuelve400() throws Exception {
        mockMvc.perform(get("/api/reportes/inexistente"))
                .andExpect(status().isBadRequest());
    }
}
