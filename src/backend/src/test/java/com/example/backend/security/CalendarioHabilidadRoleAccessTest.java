package com.example.backend.security;

import com.example.backend.controller.CalendarioDisponibilidadController;
import com.example.backend.controller.HabilidadController;
import com.example.backend.controller.VoluntarioHabilidadController;
import com.example.backend.model.Habilidad;
import com.example.backend.service.DisponibilidadService;
import com.example.backend.service.HabilidadService;
import com.example.backend.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RF-23 (KAN-30): verifica, a través de la cadena de seguridad REAL (SecurityConfig +
 * JwtAuthenticationFilter, no un MockMvc standalone), que las reglas de rol agregadas para
 * el calendario de disponibilidad y el catálogo de habilidades se cumplen exactamente como
 * se definieron en SecurityConfig. Sirve de referencia para futuras tareas que agreguen
 * endpoints relacionados con RF-23.
 */
@WebMvcTest({
        CalendarioDisponibilidadController.class,
        HabilidadController.class,
        VoluntarioHabilidadController.class
})
@Import({com.example.backend.SecurityConfig.class, JwtAuthenticationFilter.class})
class CalendarioHabilidadRoleAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DisponibilidadService disponibilidadService;
    @MockitoBean
    private HabilidadService habilidadService;
    @MockitoBean
    private JwtService jwtService;

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

    // --- GET /api/habilidades: cualquier usuario autenticado ---

    @Test
    void voluntarioPuedeListarHabilidades() throws Exception {
        when(habilidadService.listarHabilidades()).thenReturn(List.of(Habilidad.builder().idHabilidad(1).nombre("Cocina").build()));

        mockMvc.perform(get("/api/habilidades").with(user("voluntario").roles("VOLUNTARIO")))
                .andExpect(status().isOk());
    }

    // --- POST /api/habilidades: solo Administrador/TI ---

    @Test
    void gestorNoPuedeCrearHabilidades() throws Exception {
        mockMvc.perform(post("/api/habilidades")
                        .with(user("gestor").roles("GESTOR_TAREAS"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Cocina\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(habilidadService);
    }

    @Test
    void administradorPuedeCrearHabilidades() throws Exception {
        when(habilidadService.crearHabilidad("Cocina")).thenReturn(Habilidad.builder().idHabilidad(1).nombre("Cocina").build());

        mockMvc.perform(post("/api/habilidades")
                        .with(user("admin").roles("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Cocina\"}"))
                .andExpect(status().isCreated());
    }

    // --- PUT /api/voluntarios/{id}/habilidades: roles de gestión ---

    @Test
    void voluntarioNoPuedeAsignarseHabilidades() throws Exception {
        mockMvc.perform(put("/api/voluntarios/1/habilidades")
                        .with(user("voluntario").roles("VOLUNTARIO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idsHabilidades\":[1]}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(habilidadService);
    }

    @Test
    void gestorPuedeAsignarHabilidadesAUnVoluntario() throws Exception {
        when(habilidadService.asignarHabilidades(1, List.of(1))).thenReturn(List.of(Habilidad.builder().idHabilidad(1).nombre("Cocina").build()));

        mockMvc.perform(put("/api/voluntarios/1/habilidades")
                        .with(user("gestor").roles("GESTOR_TAREAS"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idsHabilidades\":[1]}"))
                .andExpect(status().isOk());
    }

    // --- GET /api/voluntarios/{id}/habilidades: cualquier autenticado (regla genérica /api/voluntarios/**) ---

    @Test
    void voluntarioPuedeConsultarHabilidadesDeUnVoluntario() throws Exception {
        when(habilidadService.obtenerHabilidadesDeVoluntario(1)).thenReturn(List.of());

        mockMvc.perform(get("/api/voluntarios/1/habilidades").with(user("voluntario").roles("VOLUNTARIO")))
                .andExpect(status().isOk());
    }
}
