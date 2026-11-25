package com.example.backend.controller;

import com.example.backend.exception.BusinessValidationException;
import com.example.backend.exception.GlobalExceptionHandler;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.DiaSemana;
import com.example.backend.service.DisponibilidadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CalendarioDisponibilidadControllerTest {

    private static final String URL = "/api/disponibilidad/calendario";

    @Mock
    private DisponibilidadService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new CalendarioDisponibilidadController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    // Criterio 1
    @Test
    void get_rangoValido_devuelveLasFranjasDeLaSemana() throws Exception {
        var ocurrencia = new DisponibilidadService.Ocurrencia(1, "Ana", "Pérez",
                LocalDate.of(2026, 11, 23), DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(12, 0));
        when(service.obtenerCalendario(eq(LocalDate.of(2026, 11, 23)), eq(LocalDate.of(2026, 11, 29)), isNull(), isNull()))
                .thenReturn(List.of(ocurrencia));

        mvc.perform(get(URL).param("desde", "2026-11-23").param("hasta", "2026-11-29"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.franjas[0].idVoluntario").value(1))
                .andExpect(jsonPath("$.franjas[0].nombreVoluntario").value("Ana"))
                .andExpect(jsonPath("$.franjas[0].diaSemana").value("LUNES"));
    }

    // Criterio 2
    @Test
    void get_conFiltroDeVoluntario_sePasaAlServicio() throws Exception {
        when(service.obtenerCalendario(any(), any(), eq(5), isNull())).thenReturn(List.of());

        mvc.perform(get(URL).param("desde", "2026-11-23").param("hasta", "2026-11-29").param("idVoluntario", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.franjas").isEmpty());
    }

    // Criterio 2
    @Test
    void get_conFiltroDeHabilidad_sePasaAlServicio() throws Exception {
        when(service.obtenerCalendario(any(), any(), isNull(), eq(3L))).thenReturn(List.of());

        mvc.perform(get(URL).param("desde", "2026-11-23").param("hasta", "2026-11-29").param("idHabilidad", "3"))
                .andExpect(status().isOk());
    }

    @Test
    void get_rangoInvalido_devuelve400() throws Exception {
        when(service.obtenerCalendario(any(), any(), isNull(), isNull()))
                .thenThrow(new BusinessValidationException("La fecha 'hasta' debe ser igual o posterior a 'desde'"));

        mvc.perform(get(URL).param("desde", "2026-11-29").param("hasta", "2026-11-23"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_voluntarioInexistente_devuelve404() throws Exception {
        when(service.obtenerCalendario(any(), any(), eq(99), isNull()))
                .thenThrow(new ResourceNotFoundException("Voluntario no encontrado con ID: 99"));

        mvc.perform(get(URL).param("desde", "2026-11-23").param("hasta", "2026-11-29").param("idVoluntario", "99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_sinFechas_devuelve400() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isBadRequest());
    }
}
