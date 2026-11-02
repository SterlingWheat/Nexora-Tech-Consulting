package com.example.backend.controller;

import com.example.backend.exception.GlobalExceptionHandler;
import com.example.backend.model.Usuario;
import com.example.backend.repository.UsuarioRepository;
import com.example.backend.service.AsignacionTareaService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AsignacionTareaControllerTest {

    @Mock
    private AsignacionTareaService service;

    @Mock
    private UsuarioRepository usuarioRepository;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(
                        new AsignacionTareaController(service, usuarioRepository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void post_cancelaAsignacionConservandoElRecurso() throws Exception {
        when(usuarioRepository.findByEmail("gestor"))
                .thenReturn(Optional.of(Usuario.builder().idUsuario(7).email("gestor").build()));

        mvc.perform(post("/api/tareas/10/asignaciones/101/cancelar")
                        .principal(new UsernamePasswordAuthenticationToken("gestor", null)))
                .andExpect(status().isNoContent());

        verify(service).cancelarAsignacion(10, 101, 7);
    }
}
