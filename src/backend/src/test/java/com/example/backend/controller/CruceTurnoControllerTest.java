package com.example.backend.controller;

import com.example.backend.exception.ConflictException;
import com.example.backend.exception.GlobalExceptionHandler;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.service.CruceTurnoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CruceTurnoControllerTest {

    private static final String URL = "/api/voluntarios/1/turnos/2/validacion-cruce";

    @Mock
    private CruceTurnoService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new CruceTurnoController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    // Criterio 3
    @Test
    void sinCruce_devuelve200Permitido() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permitido").value(true))
                .andExpect(jsonPath("$.idVoluntario").value(1))
                .andExpect(jsonPath("$.idTurno").value(2));
    }

    // Criterio 1 y 2
    @Test
    void conCruce_devuelve409ConElTurnoQueChoca() throws Exception {
        doThrow(new ConflictException("Cruce de horarios: el voluntario ya está asignado a «Clasificar» el 2026-10-28 de 08:00 a 12:00"))
                .when(service).validarSinCruces(1, 2);

        mvc.perform(get(URL))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("Clasificar")));
    }

    @Test
    void recursoInexistente_devuelve404() throws Exception {
        doThrow(new ResourceNotFoundException("Turno no encontrado con ID: 2")).when(service).validarSinCruces(1, 2);

        mvc.perform(get(URL)).andExpect(status().isNotFound());
    }
}
