package com.example.backend.controller;

import com.example.backend.exception.GlobalExceptionHandler;
import com.example.backend.model.RolEntity;
import com.example.backend.model.Usuario;
import com.example.backend.model.Voluntario;
import com.example.backend.repository.UsuarioRepository;
import com.example.backend.repository.VoluntarioRepository;
import com.example.backend.service.JwtService;
import com.example.backend.service.VoluntarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PerfilMovilControllerTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private VoluntarioRepository voluntarioRepository;
    @Mock
    private VoluntarioService voluntarioService;
    @Mock
    private JwtService jwtService;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new PerfilMovilController(
                        usuarioRepository, voluntarioRepository, voluntarioService, jwtService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getDevuelvePerfilConDocumentoYRolSinExponerLaContrasena() throws Exception {
        Usuario usuario = usuario();
        when(usuarioRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(usuario));
        when(voluntarioRepository.findByUsuarioIdUsuario(15)).thenReturn(Optional.of(voluntario(usuario)));

        mvc.perform(get("/api/movil/perfil/me")
                        .principal(new UsernamePasswordAuthenticationToken("ana@example.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("María"))
                .andExpect(jsonPath("$.documento").value("DOC-123"))
                .andExpect(jsonPath("$.rol").value("VOLUNTARIO"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void putActualizaSoloCamposPermitidosYDevuelveUnJwtConElCorreoActualizado() throws Exception {
        Usuario usuario = usuario();
        Usuario usuarioActualizado = usuario();
        usuarioActualizado.setNombre("María López");
        usuarioActualizado.setEmail("maria@example.com");
        Voluntario voluntario = voluntario(usuarioActualizado);
        when(usuarioRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(usuario));
        when(voluntarioService.actualizarPerfil(15, "María", "López", "maria@example.com", "12345678"))
                .thenReturn(voluntario);
        when(usuarioRepository.findById(15)).thenReturn(Optional.of(usuarioActualizado));
        when(jwtService.generarToken("maria@example.com", "VOLUNTARIO")).thenReturn("jwt-nuevo");

        mvc.perform(put("/api/movil/perfil/me")
                        .principal(new UsernamePasswordAuthenticationToken("ana@example.com", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre":"María",
                                  "apellido":"López",
                                  "email":"maria@example.com",
                                  "telefono":"12345678"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil.documento").value("DOC-123"))
                .andExpect(jsonPath("$.perfil.rol").value("VOLUNTARIO"))
                .andExpect(jsonPath("$.token").value("jwt-nuevo"));

        verify(voluntarioService).actualizarPerfil(
                eq(15), eq("María"), eq("López"), eq("maria@example.com"), eq("12345678"));
        verify(jwtService).generarToken("maria@example.com", "VOLUNTARIO");
    }

    @Test
    void putRechazaCorreoConFormatoInvalido() throws Exception {
        mvc.perform(put("/api/movil/perfil/me")
                        .principal(new UsernamePasswordAuthenticationToken("ana@example.com", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre":"Ana",
                                  "apellido":"López",
                                  "email":"correo-invalido",
                                  "telefono":""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private Usuario usuario() {
        return Usuario.builder()
                .idUsuario(15)
                .nombre("Ana Quispe")
                .email("ana@example.com")
                .password("secreto")
                .rol(new RolEntity(4, "VOLUNTARIO"))
                .build();
    }

    private Voluntario voluntario(Usuario usuario) {
        return Voluntario.builder()
                .idVoluntario(7)
                .usuario(usuario)
                .nombre("María")
                .apellido("López")
                .documento("DOC-123")
                .email("maria@example.com")
                .telefono("12345678")
                .build();
    }
}
