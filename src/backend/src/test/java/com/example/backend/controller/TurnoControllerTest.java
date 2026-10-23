package com.example.backend.controller;

import com.example.backend.exception.BusinessValidationException;
import com.example.backend.exception.GlobalExceptionHandler;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.Turno;
import com.example.backend.service.TurnoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TurnoControllerTest {

    private static final String VALIDO =
            "{\"fecha\":\"2026-10-25\",\"horaInicio\":\"08:00\",\"horaFin\":\"12:00\",\"actividad\":\"Clasificar alimentos\",\"cuposTotales\":5}";

    @Mock
    private TurnoService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new TurnoController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private Turno turno(int ocupados) {
        return Turno.builder().idTurno(1).fecha(LocalDate.of(2026, 10, 25)).horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(12, 0)).actividad("Clasificar alimentos").cuposTotales(5).cuposOcupados(ocupados).build();
    }

    // Criterio 1
    @Test
    void post_valido_devuelve201ConCupos() throws Exception {
        when(service.crearTurno(any())).thenReturn(turno(0));

        mvc.perform(post("/api/turnos").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idTurno").value(1))
                .andExpect(jsonPath("$.cuposTotales").value(5))
                .andExpect(jsonPath("$.cuposLibres").value(5));
    }

    // Criterio 2
    @Test
    void post_reglaDeNegocio_devuelve400ConMensaje() throws Exception {
        when(service.crearTurno(any())).thenThrow(new BusinessValidationException("La fecha del turno no puede ser pasada"));

        mvc.perform(post("/api/turnos").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("no puede ser pasada")));
    }

    @Test
    void post_camposObligatoriosFaltantes_devuelve400() throws Exception {
        mvc.perform(post("/api/turnos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("La fecha es obligatoria")))
                .andExpect(content().string(containsString("La actividad es obligatoria")))
                .andExpect(content().string(containsString("Los cupos son obligatorios")));
        verifyNoInteractions(service);
    }

    @Test
    void post_cuposCeroONegativos_devuelve400() throws Exception {
        mvc.perform(post("/api/turnos").contentType(MediaType.APPLICATION_JSON).content(VALIDO.replace("\"cuposTotales\":5", "\"cuposTotales\":0")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("al menos 1")));
        verifyNoInteractions(service);
    }

    @Test
    void post_cuposExcesivos_devuelve400() throws Exception {
        mvc.perform(post("/api/turnos").contentType(MediaType.APPLICATION_JSON).content(VALIDO.replace("\"cuposTotales\":5", "\"cuposTotales\":501")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_fechaConFormatoInvalido_devuelve400() throws Exception {
        mvc.perform(post("/api/turnos").contentType(MediaType.APPLICATION_JSON).content(VALIDO.replace("2026-10-25", "25/10/2026")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    // Criterio 3
    @Test
    void get_lista_muestraCuposOcupadosYLibres() throws Exception {
        when(service.listarTurnos()).thenReturn(List.of(turno(2)));

        mvc.perform(get("/api/turnos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cuposOcupados").value(2))
                .andExpect(jsonPath("$[0].cuposLibres").value(3));
    }

    // Criterio 3
    @Test
    void get_porId_muestraCupos() throws Exception {
        when(service.obtenerTurnoPorId(1)).thenReturn(turno(5));

        mvc.perform(get("/api/turnos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuposLibres").value(0));
    }

    @Test
    void get_porIdInexistente_devuelve404() throws Exception {
        when(service.obtenerTurnoPorId(9)).thenThrow(new ResourceNotFoundException("Turno no encontrado con ID: 9"));

        mvc.perform(get("/api/turnos/9")).andExpect(status().isNotFound());
    }
}
