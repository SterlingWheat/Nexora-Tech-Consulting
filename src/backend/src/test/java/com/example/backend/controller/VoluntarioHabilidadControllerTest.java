package com.example.backend.controller;

import com.example.backend.exception.GlobalExceptionHandler;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.Habilidad;
import com.example.backend.service.HabilidadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VoluntarioHabilidadControllerTest {

    private static final String URL = "/api/voluntarios/1/habilidades";

    @Mock
    private HabilidadService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new VoluntarioHabilidadController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void get_devuelveLasHabilidadesDelVoluntario() throws Exception {
        when(service.obtenerHabilidadesDeVoluntario(1)).thenReturn(List.of(Habilidad.builder().idHabilidad(1).nombre("Cocina").build()));

        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Cocina"));
    }

    @Test
    void put_idsValidos_reemplazaYDevuelveLasAsignadas() throws Exception {
        when(service.asignarHabilidades(eq(1), eq(List.of(10, 20))))
                .thenReturn(List.of(Habilidad.builder().idHabilidad(10).nombre("Cocina").build()));

        mvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content("{\"idsHabilidades\":[10,20]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Cocina"));
    }

    @Test
    void put_voluntarioInexistente_devuelve404() throws Exception {
        when(service.asignarHabilidades(eq(1), eq(List.of(10))))
                .thenThrow(new ResourceNotFoundException("Voluntario no encontrado con ID: 1"));

        mvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content("{\"idsHabilidades\":[10]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void put_sinIdsHabilidades_devuelve400() throws Exception {
        mvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
