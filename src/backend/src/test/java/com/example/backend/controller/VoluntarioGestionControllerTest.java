package com.example.backend.controller;

import com.example.backend.exception.ConflictException;
import com.example.backend.exception.GlobalExceptionHandler;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.DiaSemana;
import com.example.backend.model.Voluntario;
import com.example.backend.repository.UsuarioRepository;
import com.example.backend.service.VoluntarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class VoluntarioGestionControllerTest {

    private static final String VALIDO = """
            {"nombre":"Ana","apellido":"Quispe","documento":"1234567","email":"ana@correo.com","telefono":"70012345"}
            """;

    @Mock
    private VoluntarioService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new VoluntarioGestionController(service, mock(UsuarioRepository.class)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private Voluntario guardado() {
        return Voluntario.builder().idVoluntario(1).nombre("Ana").apellido("Quispe").documento("1234567")
                .email("ana@correo.com").telefono("70012345").activo(true)
                .fechaCreacion(LocalDateTime.now()).build();
    }

    // Criterio 1
    @Test
    void post_valido_devuelve201ConVoluntarioActivo() throws Exception {
        when(service.registrarVoluntario(any(), any(), any())).thenReturn(guardado());

        mvc.perform(post("/api/voluntarios").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idVoluntario").value(1))
                .andExpect(jsonPath("$.activo").value(true));
    }

    // Criterio 2
    @Test
    void post_duplicado_devuelve409ConMensajeClaro() throws Exception {
        when(service.registrarVoluntario(any(), any(), any()))
                .thenThrow(new ConflictException("Ya existe un voluntario registrado con el documento 1234567"));

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

        verify(service, never()).registrarVoluntario(any(), any(), any());
    }

    // Criterio 4
    @Test
    void post_correoConFormatoInvalido_devuelve400() throws Exception {
        String invalido = VALIDO.replace("ana@correo.com", "no-es-correo");

        mvc.perform(post("/api/voluntarios").contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("formato")));
    }

    // Criterio 4
    @Test
    void post_telefonoInvalido_devuelve400() throws Exception {
        String invalido = VALIDO.replace("70012345", "abc");

        mvc.perform(post("/api/voluntarios").contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("formato")));
    }

    // El teléfono admite como máximo 20 caracteres (columna varchar(20))
    @Test
    void post_telefonoDeMasDe20Caracteres_devuelve400() throws Exception {
        String invalido = VALIDO.replace("70012345", "1234567890123456789012");

        mvc.perform(post("/api/voluntarios").contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("formato")));

        verify(service, never()).registrarVoluntario(any(), any(), any());
    }

    @Test
    void post_telefonoDeExactamente20Caracteres_esValido() throws Exception {
        when(service.registrarVoluntario(any(), any(), any())).thenReturn(guardado());

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
                .thenThrow(new ResourceNotFoundException("Voluntario no encontrado con ID: 99"));

        mvc.perform(put("/api/voluntarios/99").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isNotFound());
    }

    @Test
    void put_duplicado_devuelve409() throws Exception {
        when(service.actualizarVoluntario(eq(1), any()))
                .thenThrow(new ConflictException("Ya existe un voluntario registrado con el correo ana@correo.com"));

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
        when(service.obtenerVoluntarioPorId(99)).thenThrow(new ResourceNotFoundException("no existe"));

        mvc.perform(get("/api/voluntarios/99/detalle")).andExpect(status().isNotFound());
    }

    // Criterio 1: baja lógica deja al voluntario inactivo conservando su historial
    @Test
    void put_desactivar_devuelve200ConVoluntarioInactivo() throws Exception {
        Voluntario inactivo = guardado();
        inactivo.setActivo(false);
        when(service.cambiarEstado(1, false)).thenReturn(inactivo);

        mvc.perform(put("/api/voluntarios/1/desactivar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idVoluntario").value(1))
                .andExpect(jsonPath("$.activo").value(false));

        verify(service).cambiarEstado(1, false);
    }

    // Criterio 3: la reactivación lo vuelve a dejar disponible
    @Test
    void put_activar_devuelve200ConVoluntarioActivo() throws Exception {
        when(service.cambiarEstado(1, true)).thenReturn(guardado());

        mvc.perform(put("/api/voluntarios/1/activar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));

        verify(service).cambiarEstado(1, true);
    }

    @Test
    void put_desactivar_inexistente_devuelve404() throws Exception {
        when(service.cambiarEstado(99, false))
                .thenThrow(new ResourceNotFoundException("Voluntario no encontrado con ID: 99"));

        mvc.perform(put("/api/voluntarios/99/desactivar")).andExpect(status().isNotFound());
    }

    // KAN-27 Criterio 1: búsqueda por texto
    @Test
    void getFiltrar_conTexto_devuelve200ConResultadosPaginados() throws Exception {
        Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("nombre").ascending());
        when(service.buscarVoluntarios(eq("ana"), isNull(), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(guardado()), pageable, 1));

        mvc.perform(get("/api/voluntarios/filtrar").param("texto", "ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].documento").value("1234567"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // KAN-27 Criterio 2: filtros de estado y disponibilidad combinados
    @Test
    void getFiltrar_conEstadoYDisponibilidad_propagaAmbosFiltros() throws Exception {
        Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("nombre").ascending());
        when(service.buscarVoluntarios(isNull(), eq(true), eq(DiaSemana.LUNES), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        mvc.perform(get("/api/voluntarios/filtrar").param("activo", "true").param("diaSemana", "LUNES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        verify(service).buscarVoluntarios(null, true, DiaSemana.LUNES, pageable);
    }

    // KAN-27 Criterio 3: paginación explícita por page/size
    @Test
    void getFiltrar_conPageYSize_losUsaParaPaginar() throws Exception {
        Pageable pageable = PageRequest.of(2, 5, org.springframework.data.domain.Sort.by("nombre").ascending());
        when(service.buscarVoluntarios(any(), any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 37));

        mvc.perform(get("/api/voluntarios/filtrar").param("page", "2").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(37))
                .andExpect(jsonPath("$.number").value(2));
    }

    // KAN-27: un tamaño de página fuera de rango se acota a un máximo razonable (protege AC3)
    @Test
    void getFiltrar_conSizeExcesivo_loAcotaA100() throws Exception {
        Pageable pageable = PageRequest.of(0, 100, org.springframework.data.domain.Sort.by("nombre").ascending());
        when(service.buscarVoluntarios(any(), any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        mvc.perform(get("/api/voluntarios/filtrar").param("size", "5000"))
                .andExpect(status().isOk());

        verify(service).buscarVoluntarios(null, null, null, pageable);
    }

    // KAN-27: un diaSemana con un valor fuera del enum es un 400 explícito, no un 500
    @Test
    void getFiltrar_conDiaSemanaInvalido_devuelve400() throws Exception {
        mvc.perform(get("/api/voluntarios/filtrar").param("diaSemana", "NOEXISTE"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}