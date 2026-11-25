package com.example.backend.controller;

import com.example.backend.exception.ConflictException;
import com.example.backend.exception.GlobalExceptionHandler;
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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class HabilidadControllerTest {

    @Mock
    private HabilidadService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new HabilidadController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void listar_devuelveElCatalogo() throws Exception {
        when(service.listarHabilidades()).thenReturn(List.of(Habilidad.builder().idHabilidad(1).nombre("Cocina").build()));

        mvc.perform(get("/api/habilidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Cocina"));
    }

    @Test
    void crear_nombreValido_devuelve201() throws Exception {
        when(service.crearHabilidad("Cocina")).thenReturn(Habilidad.builder().idHabilidad(1).nombre("Cocina").build());

        mvc.perform(post("/api/habilidades").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Cocina\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idHabilidad").value(1));
    }

    @Test
    void crear_nombreVacio_devuelve400() throws Exception {
        mvc.perform(post("/api/habilidades").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void crear_nombreDuplicado_devuelve409() throws Exception {
        when(service.crearHabilidad("Cocina")).thenThrow(new ConflictException("Ya existe una habilidad con el nombre 'Cocina'"));

        mvc.perform(post("/api/habilidades").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Cocina\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("Ya existe")));
    }
}
