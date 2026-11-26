package com.example.backend.controller;

import com.example.backend.model.HistorialTarea;
import com.example.backend.model.Tarea;
import com.example.backend.model.Usuario;
import com.example.backend.service.HistorialTareaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class HistorialTareaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private HistorialTareaService historialTareaService;

    @InjectMocks
    private HistorialTareaController historialTareaController;

    @BeforeEach
    void setUp() {
        // Configuramos MockMvc de forma "standalone" sin levantar el contexto de Spring Boot
        mockMvc = MockMvcBuilders.standaloneSetup(historialTareaController).build();
    }

    @Test
    void obtenerHistorial_DebeRetornarOkYLista_CuandoExiste() throws Exception {
        Tarea tarea = new Tarea();
        tarea.setIdTarea(10);
        
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(100);
        usuario.setNombre("Gestor Principal");

        HistorialTarea historial = new HistorialTarea();
        historial.setIdHistorial(1);
        historial.setTarea(tarea);
        historial.setUsuario(usuario);
        historial.setTipoCambio("CREACION");
        historial.setDetalle("Tarea creada con éxito");
        historial.setFechaRegistro(LocalDateTime.now());

        when(historialTareaService.obtenerHistorialPorTarea(10)).thenReturn(List.of(historial));

        mockMvc.perform(get("/api/tareas/10/historial")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].tipoCambio").value("CREACION"))
                .andExpect(jsonPath("$[0].detalle").value("Tarea creada con éxito"))
                .andExpect(jsonPath("$[0].nombreUsuario").value("Gestor Principal"));
    }

    @Test
    void obtenerHistorial_DebeRetornarOk_CuandoHistorialVacio() throws Exception {
        when(historialTareaService.obtenerHistorialPorTarea(10)).thenReturn(List.of());

        mockMvc.perform(get("/api/tareas/10/historial")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}