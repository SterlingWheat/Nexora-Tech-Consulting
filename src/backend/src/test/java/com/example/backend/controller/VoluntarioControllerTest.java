package com.example.backend.controller;

import com.example.backend.model.Voluntario;
import com.example.backend.service.VoluntarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class VoluntarioControllerTest {

    private static final String VALIDO = """
            {"nombre":"Ana","apellido":"Quispe","documento":"1234567","email":"ana@correo.com","telefono":"70012345"}
            """;

    @Mock
    private VoluntarioService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new VoluntarioController(service)).build();
    }

    private Voluntario guardado() {
        return Voluntario.builder().idVoluntario(1).nombre("Ana").apellido("Quispe").documento("1234567")
                .email("ana@correo.com").telefono("70012345").activo(true)
                .fechaCreacion(LocalDateTime.now()).build();
    }

    // Criterio 1
    @Test
    void post_valido_devuelve201ConVoluntarioActivo() throws Exception {
        when(service.registrarVoluntario(any())).thenReturn(guardado());

        mvc.perform(post("/api/voluntarios").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idVoluntario").value(1))
                .andExpect(jsonPath("$.activo").value(true));
    }

    // Criterio 2
    @Test
    void post_duplicado_devuelve409ConMensajeClaro() throws Exception {
        when(service.registrarVoluntario(any()))
                .thenThrow(new IllegalStateException("Ya existe un voluntario registrado con el documento 1234567"));

        mvc.perform(post("/api/voluntarios").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("Ya existe un voluntario")));
    }

    // Criterio 4
    @Test
    void post_camposObligatoriosVacios_devuelve400ConMensajes() throws Exception {
        String invalido = "{\"nombre\":\"\",\"apellido\":\"\",\"documento\":\"\",\"email\":\"\"}";

        mvc.perform(post("/api/voluntarios").contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("El nombre es obligatorio")))
                .andExpect(content().string(containsString("El documento es obligatorio")));
        verify(service, never()).registrarVoluntario(any());
    }

    // Criterio 4
    @Test
    void post_correoConFormatoInvalido_devuelve400() throws Exception {
        String invalido = VALIDO.replace("ana@correo.com", "no-es-correo");

        mvc.perform(post("/api/voluntarios").contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("formato válido")));
    }

    // Criterio 4
    @Test
    void post_telefonoInvalido_devuelve400() throws Exception {
        String invalido = VALIDO.replace("70012345", "abc");

        mvc.perform(post("/api/voluntarios").contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("teléfono")));
    }

    // El teléfono admite como máximo 20 caracteres (columna varchar(20))
    @Test
    void post_telefonoDeMasDe20Caracteres_devuelve400() throws Exception {
        String invalido = VALIDO.replace("70012345", "1234567890123456789012");

        mvc.perform(post("/api/voluntarios").contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("teléfono")));
        verify(service, never()).registrarVoluntario(any());
    }

    @Test
    void post_telefonoDeExactamente20Caracteres_esValido() throws Exception {
        when(service.registrarVoluntario(any())).thenReturn(guardado());
        String limite = VALIDO.replace("70012345", "12345678901234567890");

        mvc.perform(post("/api/voluntarios").contentType(MediaType.APPLICATION_JSON).content(limite))
                .andExpect(status().isCreated());
    }

    // Criterio 3
    @Test
    void put_valido_devuelve200ConFechaModificacion() throws Exception {
        Voluntario actualizado = guardado();
        actualizado.setFechaModificacion(LocalDateTime.now());
        when(service.actualizarVoluntario(eq(1), any())).thenReturn(actualizado);

        mvc.perform(put("/api/voluntarios/1").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaModificacion").exists());
    }

    @Test
    void put_inexistente_devuelve404() throws Exception {
        when(service.actualizarVoluntario(eq(99), any()))
                .thenThrow(new IllegalArgumentException("Voluntario no encontrado con ID: 99"));

        mvc.perform(put("/api/voluntarios/99").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isNotFound());
    }

    @Test
    void put_duplicado_devuelve409() throws Exception {
        when(service.actualizarVoluntario(eq(1), any()))
                .thenThrow(new IllegalStateException("Ya existe un voluntario registrado con el correo ana@correo.com"));

        mvc.perform(put("/api/voluntarios/1").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isConflict());
    }

    @Test
    void get_lista_devuelve200() throws Exception {
        when(service.listarVoluntarios()).thenReturn(List.of(guardado()));

        mvc.perform(get("/api/voluntarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documento").value("1234567"));
    }

    @Test
    void get_porIdInexistente_devuelve404() throws Exception {
        when(service.obtenerVoluntarioPorId(99)).thenThrow(new IllegalArgumentException("no existe"));

        mvc.perform(get("/api/voluntarios/99")).andExpect(status().isNotFound());
    }
}
