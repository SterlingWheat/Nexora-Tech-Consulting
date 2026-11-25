package com.example.backend.controller;

import com.example.backend.dto.HabilidadDTO;
import com.example.backend.service.HabilidadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HabilidadControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private HabilidadService habilidadService;

    @InjectMocks
    private HabilidadController habilidadController;

    private HabilidadDTO habilidadDTO;
    private HabilidadDTO savedHabilidadDTO;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(habilidadController).build();

        habilidadDTO = HabilidadDTO.builder()
                .nombre("Liderazgo")
                .descripcion("Gestión de equipos en campo")
                .build();

        savedHabilidadDTO = HabilidadDTO.builder()
                .id(1L)
                .nombre("Liderazgo")
                .descripcion("Gestión de equipos en campo")
                .build();
    }

    @Test
    void createHabilidad_ReturnsCreated() throws Exception {
        when(habilidadService.createHabilidad(any(HabilidadDTO.class))).thenReturn(savedHabilidadDTO);

        mockMvc.perform(post("/api/admin/habilidades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(habilidadDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Liderazgo"));
    }

    @Test
    void updateHabilidad_ReturnsOk() throws Exception {
        when(habilidadService.updateHabilidad(eq(1L), any(HabilidadDTO.class))).thenReturn(savedHabilidadDTO);

        mockMvc.perform(put("/api/admin/habilidades/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(habilidadDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Liderazgo"));
    }

    @Test
    void deleteHabilidad_ReturnsNoContent() throws Exception {
        doNothing().when(habilidadService).deleteHabilidad(1L);

        mockMvc.perform(delete("/api/admin/habilidades/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void getHabilidadById_ReturnsOk() throws Exception {
        when(habilidadService.getHabilidadById(1L)).thenReturn(savedHabilidadDTO);

        mockMvc.perform(get("/api/admin/habilidades/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Liderazgo"));
    }

    @Test
    void getAllHabilidades_ReturnsOk() throws Exception {
        when(habilidadService.getAllHabilidades()).thenReturn(List.of(savedHabilidadDTO));

        mockMvc.perform(get("/api/admin/habilidades")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Liderazgo"));
    }
}