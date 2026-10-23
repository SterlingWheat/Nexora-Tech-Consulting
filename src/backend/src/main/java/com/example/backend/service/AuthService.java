package com.example.backend.service;

import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.LoginResponse;
import com.example.backend.exception.UsuarioInactivoException;
import com.example.backend.model.Usuario;
import com.example.backend.repository.UsuarioRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Credenciales inválidas";
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResponse iniciarSesion(LoginRequest request) {
        if (request == null || request.getEmail() == null || request.getPassword() == null
                || request.getEmail().isBlank() || request.getPassword().isBlank()) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        String emailBuscado = request.getEmail().trim();
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(emailBuscado);

        if (usuarioOpt.isEmpty()) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        Usuario usuario = usuarioOpt.get();

        // Solución Error 3: Se lanza una excepción específica para que el AuthController 
        // pueda responder con un mensaje personalizado indicando que la cuenta está inactiva.
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new UsuarioInactivoException("Usuario Inactivo. Tu cuenta ha sido deshabilitada.");
        }

        if (!coincidePassword(request.getPassword(), usuario)) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        String nombreRol = (usuario.getRol() != null && usuario.getRol().getNombre() != null)
                ? usuario.getRol().getNombre()
                : "VOLUNTARIO";

        String token = jwtService.generarToken(usuario.getEmail(), nombreRol);
        return new LoginResponse(token, nombreRol);
    }

    private boolean coincidePassword(String rawPassword, Usuario usuario) {
        String storedPassword = usuario.getPassword();
        if (storedPassword == null) {
            return false;
        }
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        if (storedPassword.startsWith("{")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        if (!storedPassword.equals(rawPassword)) {
            return false;
        }
        usuario.setPassword(passwordEncoder.encode(rawPassword));
        usuarioRepository.save(usuario);
        return true;
    }
}