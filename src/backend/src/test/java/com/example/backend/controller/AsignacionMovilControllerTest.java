package com.example.backend.controller;

import com.example.backend.dto.AsignacionTareaMovilDTO;
import com.example.backend.exception.GlobalExceptionHandler;
import com.example.backend.model.Usuario;
import com.example.backend.model.Voluntario;
import com.example.backend.repository.UsuarioRepository;
import com.example.backend.repository.VoluntarioRepository;
import com.example.backend.service.AsignacionTareaService;
import com.example.backend.service.AsignacionTurnoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AsignacionMovilControllerTest {

    @Mock
    private AsignacionTareaService tareaService;
    @Mock
    private AsignacionTurnoService turnoService;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private VoluntarioRepository voluntarioRepository;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new AsignacionMovilController(
                        tareaService, turnoService, usuarioRepository, voluntarioRepository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void patchConfirmaAsignacionDeTareaDelVoluntarioAutenticado() throws Exception {
        when(usuarioRepository.findByEmail("ana@example.com"))
                .thenReturn(Optional.of(Usuario.builder().idUsuario(15).email("ana@example.com").build()));
        when(voluntarioRepository.findByUsuarioIdUsuario(15))
                .thenReturn(Optional.of(Voluntario.builder().idVoluntario(5).build()));

        mvc.perform(patch("/api/movil/asignaciones/tareas/10/101/confirmar")
                        .principal(new UsernamePasswordAuthenticationToken("ana@example.com", null)))
                .andExpect(status().isNoContent());

        verify(tareaService).responderAsignacion(10, 101, 5, 15, true);
    }

    @Test
    void patchRechazaAsignacionDeTurnoDelVoluntarioAutenticado() throws Exception {
        when(usuarioRepository.findByEmail("ana@example.com"))
                .thenReturn(Optional.of(Usuario.builder().idUsuario(15).email("ana@example.com").build()));
        when(voluntarioRepository.findByUsuarioIdUsuario(15))
                .thenReturn(Optional.of(Voluntario.builder().idVoluntario(5).build()));

        mvc.perform(patch("/api/movil/asignaciones/turnos/8/30/rechazar")
                        .principal(new UsernamePasswordAuthenticationToken("ana@example.com", null)))
                .andExpect(status().isNoContent());

        verify(turnoService).responderAsignacion(8, 30L, 5, false);
    }
}