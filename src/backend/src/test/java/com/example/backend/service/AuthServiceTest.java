package com.example.backend.service;

import com.example.backend.dto.LoginRequest;
import com.example.backend.exception.UsuarioInactivoException;
import com.example.backend.model.RolEntity;
import com.example.backend.model.Usuario;
import com.example.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UsuarioRepository usuarioRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        authService = new AuthService(usuarioRepository, passwordEncoder, jwtService);
    }

    @Test
    void iniciaSesionConCredencialesValidas() {
        Usuario usuario = usuario("{bcrypt}hash", true);
        when(usuarioRepository.findByEmail("persona@example.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("correcta", "{bcrypt}hash")).thenReturn(true);
        when(jwtService.generarToken("persona@example.com", "VOLUNTARIO")).thenReturn("jwt-firmado");

        var response = authService.iniciarSesion(
                loginRequest("persona@example.com", "correcta"));

        assertEquals("jwt-firmado", response.getToken());
        assertEquals("VOLUNTARIO", response.getRol());
    }

    @Test
    void rechazaUsuarioInexistenteConErrorGenerico() {
        when(usuarioRepository.findByEmail("persona@example.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class,
                () -> authService.iniciarSesion(loginRequest("persona@example.com", "incorrecta")));
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void rechazaContrasenaIncorrectaConErrorGenerico() {
        Usuario usuario = usuario("{bcrypt}hash", true);
        when(usuarioRepository.findByEmail("persona@example.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("incorrecta", "{bcrypt}hash")).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> authService.iniciarSesion(loginRequest("persona@example.com", "incorrecta")));
    }

    @Test
    void rechazaUsuarioInactivo() {
        Usuario usuario = usuario("{bcrypt}hash", false);
        when(usuarioRepository.findByEmail("persona@example.com")).thenReturn(Optional.of(usuario));

        assertThrows(UsuarioInactivoException.class,
                () -> authService.iniciarSesion(loginRequest("persona@example.com", "correcta")));
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void convierteContrasenaLegadaAlFormatoCodificadoDespuesDeUnLoginValido() {
        Usuario usuario = usuario("contrasena-legada", true);
        when(usuarioRepository.findByEmail("persona@example.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("contrasena-legada")).thenReturn("{bcrypt}hash-nuevo");
        when(jwtService.generarToken("persona@example.com", "VOLUNTARIO")).thenReturn("jwt-firmado");

        authService.iniciarSesion(loginRequest("persona@example.com", "contrasena-legada"));

        assertEquals("{bcrypt}hash-nuevo", usuario.getPassword());
        verify(usuarioRepository).save(usuario);
    }

    private Usuario usuario(String password, boolean activo) {
        return Usuario.builder()
                .email("persona@example.com")
                .password(password)
                .rol(new RolEntity(4, "VOLUNTARIO"))
                .estado(activo ? "ACTIVO" : "INACTIVO")
                .build();
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }
}