package com.example.backend.security;

import com.example.backend.controller.TurnoController;
import com.example.backend.repository.UsuarioRepository;
import com.example.backend.service.TurnoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RF-30 (KAN-53): verifica, a través de la cadena de seguridad REAL (SecurityConfig +
 * JwtAuthenticationFilter), que el calendario general de turnos queda restringido a
 * ADMINISTRADOR/GESTOR_TAREAS (la misma regla que ya protege el historial de asignaciones de
 * un turno), y que un voluntario o un cliente sin autenticar no pueden consultarlo.
 */
@WebMvcTest(TurnoController.class)
@Import({com.example.backend.SecurityConfig.class, JwtAuthenticationFilter.class})
class CalendarioTurnosRoleAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TurnoService turnoService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;
    @MockitoBean
    private com.example.backend.service.JwtService jwtService;

    @Test
    void administradorPuedeConsultarElCalendarioDeTurnos() throws Exception {
        when(turnoService.listarTurnos(any(), any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/turnos/calendario")
                        .param("desde", "2026-11-20").param("hasta", "2026-11-26")
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk());
    }

    @Test
    void gestorPuedeConsultarElCalendarioDeTurnos() throws Exception {
        when(turnoService.listarTurnos(any(), any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/turnos/calendario")
                        .param("desde", "2026-11-20").param("hasta", "2026-11-26")
                        .with(user("gestor").roles("GESTOR_TAREAS")))
                .andExpect(status().isOk());
    }

    @Test
    void voluntarioNoPuedeConsultarElCalendarioDeTurnos() throws Exception {
        mockMvc.perform(get("/api/turnos/calendario")
                        .param("desde", "2026-11-20").param("hasta", "2026-11-26")
                        .with(user("voluntario").roles("VOLUNTARIO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void tiNoPuedeConsultarElCalendarioDeTurnos() throws Exception {
        mockMvc.perform(get("/api/turnos/calendario")
                        .param("desde", "2026-11-20").param("hasta", "2026-11-26")
                        .with(user("ti").roles("TI")))
                .andExpect(status().isForbidden());
    }

    @Test
    void noAutenticado_recibe401() throws Exception {
        mockMvc.perform(get("/api/turnos/calendario")
                        .param("desde", "2026-11-20").param("hasta", "2026-11-26"))
                .andExpect(status().isUnauthorized());
    }

    // El listado plano (sin rango) sigue abierto a cualquier autenticado, como ya estaba
    @Test
    void voluntarioSiguePudiendoConsultarElListadoPlanoDeTurnos() throws Exception {
        when(turnoService.listarTurnos()).thenReturn(List.of());

        mockMvc.perform(get("/api/turnos").with(user("voluntario").roles("VOLUNTARIO")))
                .andExpect(status().isOk());
    }
}
