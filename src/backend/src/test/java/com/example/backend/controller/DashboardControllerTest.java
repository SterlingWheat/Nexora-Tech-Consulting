package com.example.backend.controller;

import com.example.backend.dto.ConteoPorEstadoDTO;
import com.example.backend.dto.DashboardMetricsDTO;
import com.example.backend.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private DashboardService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new DashboardController(service)).build();
    }

    // RF-44 Criterio 1
    @Test
    void getMetricas_devuelveLosCuatroIndicadores() throws Exception {
        when(service.obtenerMetricas()).thenReturn(new DashboardMetricsDTO(
                15L, 5L, 7L,
                List.of(new ConteoPorEstadoDTO("Pendiente", 3L)),
                4L, 6L));

        mvc.perform(get("/api/dashboard/metricas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voluntariosActivos").value(15))
                .andExpect(jsonPath("$.voluntariosInactivos").value(5))
                .andExpect(jsonPath("$.tareasAbiertas").value(7))
                .andExpect(jsonPath("$.tareasPorEstado[0].estado").value("Pendiente"))
                .andExpect(jsonPath("$.tareasPorEstado[0].cantidad").value(3))
                .andExpect(jsonPath("$.turnosProximos").value(4))
                .andExpect(jsonPath("$.asignacionesPendientes").value(6));
    }
}
