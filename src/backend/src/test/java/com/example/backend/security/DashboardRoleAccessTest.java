package com.example.backend.security;

import com.example.backend.controller.DashboardController;
import com.example.backend.dto.DashboardMetricsDTO;
import com.example.backend.service.DashboardService;
import com.example.backend.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RF-44 (KAN-49): verifica, a través de la cadena de seguridad REAL (SecurityConfig +
 * JwtAuthenticationFilter), que el panel administrativo queda restringido exclusivamente
 * a ADMINISTRADOR, tal como dice la historia ("Como Administrador, quiero...").
 */
@WebMvcTest(DashboardController.class)
@Import({com.example.backend.SecurityConfig.class, JwtAuthenticationFilter.class})
class DashboardRoleAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;
    @MockitoBean
    private JwtService jwtService;

    @Test
    void administradorPuedeConsultarLasMetricas() throws Exception {
        when(dashboardService.obtenerMetricas()).thenReturn(
                new DashboardMetricsDTO(0L, 0L, 0L, List.of(), 0L, 0L));

        mockMvc.perform(get("/api/dashboard/metricas").with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk());
    }

    @Test
    void gestorNoPuedeConsultarLasMetricas() throws Exception {
        mockMvc.perform(get("/api/dashboard/metricas").with(user("gestor").roles("GESTOR_TAREAS")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(dashboardService);
    }

    @Test
    void tiNoPuedeConsultarLasMetricas() throws Exception {
        mockMvc.perform(get("/api/dashboard/metricas").with(user("ti").roles("TI")))
                .andExpect(status().isForbidden());
    }

    @Test
    void voluntarioNoPuedeConsultarLasMetricas() throws Exception {
        mockMvc.perform(get("/api/dashboard/metricas").with(user("voluntario").roles("VOLUNTARIO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void noAutenticado_recibe401() throws Exception {
        mockMvc.perform(get("/api/dashboard/metricas"))
                .andExpect(status().isUnauthorized());
    }
}
