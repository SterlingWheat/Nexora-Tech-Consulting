package com.example.backend.controller;

import com.example.backend.dto.NotificacionMovilDTO;
import com.example.backend.exception.GlobalExceptionHandler;
import com.example.backend.service.CentroNotificacionesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CentroNotificacionesMovilControllerTest {

    @Mock
    private CentroNotificacionesService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new CentroNotificacionesMovilController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getListaAvisosDelVoluntarioAutenticado() throws Exception {
        when(service.listar("ana@example.com")).thenReturn(List.of(
                new NotificacionMovilDTO(
                        2L,
                        "Asignación",
                        "Apoyar en la actividad",
                        "25/11/2026 10:00",
                        LocalDateTime.of(2026, 11, 21, 19, 30),
                        false)));

        mvc.perform(get("/api/movil/notificaciones")
                        .principal(new UsernamePasswordAuthenticationToken("ana@example.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].actividad").value("Apoyar en la actividad"))
                .andExpect(jsonPath("$[0].leida").value(false));

        verify(service).listar("ana@example.com");
    }

    @Test
    void patchMarcaComoLeidoSoloElAvisoSolicitado() throws Exception {
        mvc.perform(patch("/api/movil/notificaciones/2/leida")
                        .principal(new UsernamePasswordAuthenticationToken("ana@example.com", null)))
                .andExpect(status().isNoContent());

        verify(service).marcarLeida(2L, "ana@example.com");
    }
}
