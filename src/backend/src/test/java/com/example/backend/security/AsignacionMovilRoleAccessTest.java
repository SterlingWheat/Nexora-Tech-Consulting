package com.example.backend.security;

import com.example.backend.controller.AsignacionMovilController;
import com.example.backend.model.Usuario;
import com.example.backend.model.Voluntario;
import com.example.backend.repository.UsuarioRepository;
import com.example.backend.repository.VoluntarioRepository;
import com.example.backend.service.AsignacionTareaService;
import com.example.backend.service.AsignacionTurnoService;
import com.example.backend.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RF-38/39 (KAN-35): verifica, a través de la cadena de seguridad REAL (SecurityConfig +
 * JwtAuthenticationFilter), que la consulta móvil de tareas y turnos sigue restringida
 * exclusivamente al rol VOLUNTARIO (regla "/api/movil/**" -> hasRole("VOLUNTARIO")), y que
 * ningún otro rol de gestión ni un cliente sin autenticar puede leerla.
 */
@WebMvcTest(AsignacionMovilController.class)
@Import({com.example.backend.SecurityConfig.class, JwtAuthenticationFilter.class})
class AsignacionMovilRoleAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AsignacionTareaService tareaService;
    @MockitoBean
    private AsignacionTurnoService turnoService;
    @MockitoBean
    private UsuarioRepository usuarioRepository;
    @MockitoBean
    private VoluntarioRepository voluntarioRepository;
    @MockitoBean
    private JwtService jwtService;

    private void stubIdentidad() {
        when(usuarioRepository.findByEmail("voluntario"))
                .thenReturn(Optional.of(Usuario.builder().idUsuario(1).email("voluntario").build()));
        when(voluntarioRepository.findByUsuarioIdUsuario(1))
                .thenReturn(Optional.of(Voluntario.builder().idVoluntario(1).build()));
    }

    @Test
    void voluntarioPuedeConsultarSusTareas() throws Exception {
        stubIdentidad();
        when(tareaService.obtenerAsignacionesDelVoluntario(1)).thenReturn(List.of());

        mockMvc.perform(get("/api/movil/asignaciones/tareas").with(user("voluntario").roles("VOLUNTARIO")))
                .andExpect(status().isOk());
    }

    @Test
    void voluntarioPuedeConsultarSusTurnos() throws Exception {
        stubIdentidad();
        when(turnoService.obtenerAsignacionesDelVoluntario(1)).thenReturn(List.of());

        mockMvc.perform(get("/api/movil/asignaciones/turnos").with(user("voluntario").roles("VOLUNTARIO")))
                .andExpect(status().isOk());
    }

    @Test
    void gestorNoPuedeConsultarLasTareasMoviles() throws Exception {
        mockMvc.perform(get("/api/movil/asignaciones/tareas").with(user("gestor").roles("GESTOR_TAREAS")))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorNoPuedeConsultarLosTurnosMoviles() throws Exception {
        mockMvc.perform(get("/api/movil/asignaciones/turnos").with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void noAutenticado_recibe401() throws Exception {
        mockMvc.perform(get("/api/movil/asignaciones/tareas"))
                .andExpect(status().isUnauthorized());
    }
}
