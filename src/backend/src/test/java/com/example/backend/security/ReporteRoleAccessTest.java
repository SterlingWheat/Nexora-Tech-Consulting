package com.example.backend.security;

import com.example.backend.controller.ReporteController;
import com.example.backend.dto.ReporteTablaDTO;
import com.example.backend.service.ExcelExportService;
import com.example.backend.service.PdfExportService;
import com.example.backend.service.ReporteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// RF-47 (KAN-58): la exportación de reportes es exclusiva del Administrador, verificado
// contra la cadena de seguridad real (SecurityConfig + JwtAuthenticationFilter).
@WebMvcTest(ReporteController.class)
@Import({com.example.backend.SecurityConfig.class, JwtAuthenticationFilter.class})
class ReporteRoleAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReporteService reporteService;
    @MockitoBean
    private PdfExportService pdfExportService;
    @MockitoBean
    private ExcelExportService excelExportService;
    @MockitoBean
    private com.example.backend.service.JwtService jwtService;

    @Test
    void administradorPuedeExportarReportesAPdf() throws Exception {
        when(reporteService.reporteVoluntarios()).thenReturn(new ReporteTablaDTO("t", List.of(), List.of()));
        when(pdfExportService.exportar(any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/reportes/voluntarios/pdf").with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk());
    }

    @Test
    void administradorPuedeExportarReportesAExcel() throws Exception {
        when(reporteService.reporteTareas()).thenReturn(new ReporteTablaDTO("t", List.of(), List.of()));
        when(excelExportService.exportar(any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/reportes/tareas/excel").with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk());
    }

    @Test
    void gestorDeTareasNoPuedeExportarReportes() throws Exception {
        mockMvc.perform(get("/api/reportes/turnos/pdf").with(user("gestor").roles("GESTOR_TAREAS")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(reporteService, pdfExportService);
    }

    @Test
    void voluntarioNoPuedeExportarReportes() throws Exception {
        mockMvc.perform(get("/api/reportes/voluntarios/excel").with(user("voluntario").roles("VOLUNTARIO")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(reporteService, excelExportService);
    }

    @Test
    void noAutenticado_recibe401AlExportarReportes() throws Exception {
        mockMvc.perform(get("/api/reportes/voluntarios/pdf"))
                .andExpect(status().isUnauthorized());
    }
}
