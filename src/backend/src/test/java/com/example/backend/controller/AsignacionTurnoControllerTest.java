package com.example.backend.controller;

import com.example.backend.exception.GlobalExceptionHandler;
import com.example.backend.model.TurnoAssignmentRecord;
import com.example.backend.service.AsignacionTurnoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AsignacionTurnoControllerTest {

    @Mock
    private AsignacionTurnoService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new AsignacionTurnoController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void post_asignaVoluntariosYDevuelveNuevosRegistros() throws Exception {
        when(service.asignarVoluntarios(8, List.of(5))).thenReturn(List.of(
                new TurnoAssignmentRecord(30L, 8, 5, "Ana Paz", "ana@example.com", "Pendiente")));

        mvc.perform(post("/api/turnos/8/asignaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idVoluntarios\":[5]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].idAsignacion").value(30))
                .andExpect(jsonPath("$[0].nombreVoluntario").value("Ana Paz"))
                .andExpect(jsonPath("$[0].estado").value("Pendiente"));
    }

    @Test
    void get_devuelveHistorialDeAsignaciones() throws Exception {
        when(service.listarAsignaciones(8)).thenReturn(List.of(
                new TurnoAssignmentRecord(30L, 8, 5, "Ana Paz", "ana@example.com", "Cancelada")));

        mvc.perform(get("/api/turnos/8/asignaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idAsignacion").value(30))
                .andExpect(jsonPath("$[0].estado").value("Cancelada"));
    }

    @Test
    void post_rechazaListaVaciaAntesDeLlamarAlServicio() throws Exception {
        mvc.perform(post("/api/turnos/8/asignaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idVoluntarios\":[]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void post_cancelaAsignacionConservandoSuRegistro() throws Exception {
        mvc.perform(post("/api/turnos/8/asignaciones/30/cancelar"))
                .andExpect(status().isNoContent());

        verify(service).cancelarAsignacion(8, 30L);
    }
}
