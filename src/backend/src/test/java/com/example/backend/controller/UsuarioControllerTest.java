package com.example.backend.controller;

import com.example.backend.model.RolEntity;
import com.example.backend.model.Usuario;
import com.example.backend.repository.RolRepository;
import com.example.backend.repository.UsuarioRepository;
import com.example.backend.repository.VoluntarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class UsuarioControllerTest {

    private UsuarioRepository usuarioRepository;
    private RolRepository rolRepository;
    private PasswordEncoder passwordEncoder;
    private VoluntarioRepository voluntarioRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        rolRepository = mock(RolRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        voluntarioRepository = mock(VoluntarioRepository.class);

        when(rolRepository.findByNombre(anyString())).thenAnswer(inv -> 
                Optional.of(new RolEntity(1, inv.getArgument(0))));

        mockMvc = standaloneSetup(new UsuarioController(usuarioRepository, rolRepository, passwordEncoder, voluntarioRepository)).build();
    }

    @Test
    void almacenaLaContrasenaCodificadaYNoLaIncluyeEnLaRespuesta() throws Exception {
        when(usuarioRepository.existsByEmailIgnoreCase("persona@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password-seguro")).thenReturn("{bcrypt}hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre":"Persona",
                                  "email":"persona@example.com",
                                  "password":"password-seguro",
                                  "rol":"VOLUNTARIO"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "nombre":"Persona",
                          "email":"persona@example.com",
                          "rol":"VOLUNTARIO",
                          "activo":true
                        }
                        """))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("password"))));

        verify(passwordEncoder).encode("password-seguro");
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void impideCrearUnaCuentaConCorreoYaRegistradoSinImportarMayusculas() throws Exception {
        when(usuarioRepository.existsByEmailIgnoreCase("persona@example.com")).thenReturn(true);

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre":"Otra Persona",
                                  "email":"Persona@Example.com",
                                  "password":"password-seguro",
                                  "rol":"VOLUNTARIO"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().string("El correo ya está registrado."));
    }

    @Test
    void actualizaUsuarioYConservaContrasenaCuandoNoSeEnviaUnaNueva() throws Exception {
        Usuario usuario = Usuario.builder()
                .idUsuario(7)
                .nombre("Nombre anterior")
                .email("anterior@example.com")
                .password("{bcrypt}hash")
                .rol(new RolEntity(4, "VOLUNTARIO"))
                .estado("ACTIVO")
                .build();

        when(usuarioRepository.findById(7)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByEmailIgnoreCaseAndIdUsuarioNot("nuevo@example.com", 7))
                .thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/usuarios/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre":"Nombre actualizado",
                                  "email":"Nuevo@Example.com",
                                  "password":"",
                                  "rol":"GESTOR_TAREAS"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "idUsuario":7,
                          "nombre":"Nombre actualizado",
                          "email":"nuevo@example.com",
                          "rol":"GESTOR_TAREAS",
                          "activo":true
                        }
                        """))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString("password"))));

        org.junit.jupiter.api.Assertions.assertEquals("{bcrypt}hash", usuario.getPassword());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void noPermiteAsignarUnCorreoDeOtraCuentaAlEditar() throws Exception {
        Usuario usuario = Usuario.builder()
                .idUsuario(7)
                .nombre("Persona")
                .email("persona@example.com")
                .password("{bcrypt}hash")
                .rol(new RolEntity(4, "VOLUNTARIO"))
                .estado("ACTIVO")
                .build();

        when(usuarioRepository.findById(7)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByEmailIgnoreCaseAndIdUsuarioNot("ocupado@example.com", 7))
                .thenReturn(true);

        mockMvc.perform(put("/api/usuarios/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre":"Persona",
                                  "email":"ocupado@example.com",
                                  "rol":"VOLUNTARIO"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().string("El correo ya está registrado."));
    }

    @Test
    void filtraListadoPorRolYEstado() throws Exception {
        Usuario activo = Usuario.builder()
                .idUsuario(1)
                .nombre("Ana")
                .email("ana@example.com")
                .password("{bcrypt}hash")
                .rol(new RolEntity(4, "VOLUNTARIO"))
                .estado("ACTIVO")
                .build();

        Usuario inactivo = Usuario.builder()
                .idUsuario(2)
                .nombre("Luis")
                .email("luis@example.com")
                .password("{bcrypt}hash")
                .rol(new RolEntity(4, "VOLUNTARIO"))
                .estado("INACTIVO")
                .build();

        when(usuarioRepository.findAll()).thenReturn(java.util.List.of(activo, inactivo));

        mockMvc.perform(get("/api/usuarios").param("rol", "VOLUNTARIO").param("activo", "false"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{
                          "idUsuario":2,
                          "nombre":"Luis",
                          "email":"luis@example.com",
                          "rol":"VOLUNTARIO",
                          "activo":false
                        }]
                        """));
    }

    @Test
    void desactivaLaCuentaSinEliminarElRegistro() throws Exception {
        Usuario usuario = Usuario.builder()
                .idUsuario(7)
                .nombre("Ana")
                .email("ana@example.com")
                .password("{bcrypt}hash")
                .rol(new RolEntity(4, "VOLUNTARIO"))
                .estado("ACTIVO")
                .build();

        when(usuarioRepository.findById(7)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        mockMvc.perform(put("/api/usuarios/7/desactivar"))
                .andExpect(status().isOk())
                .andExpect(content().string("Usuario desactivado correctamente."));

        org.junit.jupiter.api.Assertions.assertFalse(usuario.getActivo());
        verify(usuarioRepository).save(usuario);
    }
}