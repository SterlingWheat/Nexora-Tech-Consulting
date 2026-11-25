package com.example.backend.security;

import com.example.backend.controller.CalendarioDisponibilidadController;
import com.example.backend.service.DisponibilidadService;
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

/**
 * RF-23 (KAN-30): verifica, a través de la cadena de seguridad REAL (SecurityConfig +
 * JwtAuthenticationFilter, no un MockMvc standalone), que las reglas de rol agregadas para
 * el calendario de disponibilidad se cumplen exactamente como se definieron en SecurityConfig.
 *
 * El catálogo de habilidades (filtro por idHabilidad que usa este calendario) ya existe de
 * forma independiente en el proyecto (KAN 39-40, en /api/admin/habilidades), con su propia
 * seguridad vía @PreAuthorize en HabilidadController; no se duplica aquí.
 */
@WebMvcTest(CalendarioDisponibilidadController.class)
@Import({com.example.backend.SecurityConfig.class, JwtAuthenticationFilter.class})
class CalendarioHabilidadRoleAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DisponibilidadService disponibilidadService;
    @MockitoBean
    private com.example.backend.service.JwtService jwtService;

    // --- GET /api/disponibilidad/calendario: solo roles de gestión ---

    @Test
    void voluntarioNoPuedeConsultarElCalendario() throws Exception {
        mockMvc.perform(get("/api/disponibilidad/calendario")
                        .param("desde", "2026-11-23").param("hasta", "2026-11-29")
                        .with(user("voluntario").roles("VOLUNTARIO")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(disponibilidadService);
    }

    @Test
    void gestorPuedeConsultarElCalendario() throws Exception {
        when(disponibilidadService.obtenerCalendario(any(), any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/disponibilidad/calendario")
                        .param("desde", "2026-11-23").param("hasta", "2026-11-29")
                        .with(user("gestor").roles("GESTOR_TAREAS")))
                .andExpect(status().isOk());
    }

    @Test
    void administradorPuedeConsultarElCalendario() throws Exception {
        when(disponibilidadService.obtenerCalendario(any(), any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/disponibilidad/calendario")
                        .param("desde", "2026-11-23").param("hasta", "2026-11-29")
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk());
    }

    @Test
    void tiPuedeConsultarElCalendario() throws Exception {
        when(disponibilidadService.obtenerCalendario(any(), any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/disponibilidad/calendario")
                        .param("desde", "2026-11-23").param("hasta", "2026-11-29")
                        .with(user("ti").roles("TI")))
                .andExpect(status().isOk());
    }

    @Test
    void noAutenticado_recibe401AlConsultarElCalendario() throws Exception {
        mockMvc.perform(get("/api/disponibilidad/calendario")
                        .param("desde", "2026-11-23").param("hasta", "2026-11-29"))
                .andExpect(status().isUnauthorized());
    }
}
