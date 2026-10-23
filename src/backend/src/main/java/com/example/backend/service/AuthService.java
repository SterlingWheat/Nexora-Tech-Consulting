package com.example.backend.service;

import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.LoginResponse;
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

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(request.getEmail().trim());
        if (usuarioOpt.isEmpty()) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        Usuario usuario = usuarioOpt.get();
        if (!Boolean.TRUE.equals(usuario.getActivo())
                || !coincidePassword(request.getPassword(), usuario)) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        String token = jwtService.generarToken(usuario.getEmail(), usuario.getRol().name());
        return new LoginResponse(token, usuario.getRol().name());
    }

    private boolean coincidePassword(String rawPassword, Usuario usuario) {
        String storedPassword = usuario.getPassword();
        if (storedPassword == null) {
            return false;
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
