package com.example.backend.controller;

import com.example.backend.exception.BusinessValidationException;
import com.example.backend.exception.GlobalExceptionHandler;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.DiaSemana;
import com.example.backend.model.Disponibilidad;
import com.example.backend.repository.VoluntarioRepository;
import com.example.backend.service.DisponibilidadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DisponibilidadControllerTest {

    private static final String URL = "/api/voluntarios/1/disponibilidad";
    private static final String VALIDO =
            "{\"franjas\":[{\"diaSemana\":\"LUNES\",\"horaInicio\":\"08:00\",\"horaFin\":\"12:00\"}]}";

    @Mock
    private DisponibilidadService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new DisponibilidadController(service, mock(VoluntarioRepository.class)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private Disponibilidad lunes() {
        return Disponibilidad.builder().diaSemana(DiaSemana.LUNES)
                .horaInicio(LocalTime.of(8, 0)).horaFin(LocalTime.of(12, 0)).build();
    }

    // Criterio 1
    @Test
    void put_valido_devuelve200ConLasFranjas() throws Exception {
        when(service.reemplazarDisponibilidad(eq(1), any()))
                .thenReturn(new DisponibilidadService.Resultado(List.of(lunes()), List.of()));

        mvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idVoluntario").value(1))
                .andExpect(jsonPath("$.franjas[0].diaSemana").value("LUNES"))
                .andExpect(jsonPath("$.franjas[0].horaInicio").value("08:00:00"))
                .andExpect(jsonPath("$.advertencias").isEmpty());
    }

    // Criterio 2
    @Test
    void put_horaFinAnterior_devuelve400ConMensaje() throws Exception {
        when(service.reemplazarDisponibilidad(eq(1), any()))
                .thenThrow(new BusinessValidationException("La hora de fin (10:00) debe ser posterior a la hora de inicio (12:00)"));

        mvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("debe ser posterior")));
    }

    // Criterio 3
    @Test
    void put_conUnificacion_devuelveAdvertencias() throws Exception {
        when(service.reemplazarDisponibilidad(eq(1), any()))
                .thenReturn(new DisponibilidadService.Resultado(List.of(lunes()), List.of("Se unificaron franjas superpuestas del LUNES")));

        mvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.advertencias[0]").value(containsString("LUNES")));
    }

    @Test
    void put_sinFranjas_devuelve400() throws Exception {
        mvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("franjas es obligatoria")));

        verifyNoInteractions(service);
    }

    @Test
    void put_franjaIncompleta_devuelve400() throws Exception {
        String incompleta = "{\"franjas\":[{\"diaSemana\":\"LUNES\",\"horaInicio\":\"08:00\"}]}";
        mvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(incompleta))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("hora de fin")));

        verifyNoInteractions(service);
    }

    @Test
    void put_diaInvalido_devuelve400() throws Exception {
        String invalido = VALIDO.replace("LUNES", "FUNDAY");
        mvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void put_horaConFormatoInvalido_devuelve400() throws Exception {
        String invalido = VALIDO.replace("08:00", "ocho");
        mvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest());
    }

    @Test
    void put_masDe50Franjas_devuelve400() throws Exception {
        String franja = "{\"diaSemana\":\"LUNES\",\"horaInicio\":\"08:00\",\"horaFin\":\"09:00\"}";
        String body = "{\"franjas\":[" + String.join(",", java.util.Collections.nCopies(51, franja)) + "]}";

        mvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("50 franjas")));
    }

    @Test
    void put_voluntarioInexistente_devuelve404() throws Exception {
        when(service.reemplazarDisponibilidad(eq(1), any())).thenThrow(new ResourceNotFoundException("Voluntario no encontrado con ID: 1"));

        mvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_devuelveLaDisponibilidadDelPerfil() throws Exception {
        when(service.obtenerDisponibilidad(1)).thenReturn(List.of(lunes()));

        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.franjas.length()").value(1));
    }

    @Test
    void get_voluntarioInexistente_devuelve404() throws Exception {
        when(service.obtenerDisponibilidad(1)).thenThrow(new ResourceNotFoundException("no existe"));

        mvc.perform(get(URL)).andExpect(status().isNotFound());
    }
}