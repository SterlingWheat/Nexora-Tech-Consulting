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
            System.out.println(">>> [AUTH ERROR] Petición vacía o campos nulos");
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        String emailBuscado = request.getEmail().trim();
        System.out.println(">>> [AUTH] Intentando login para correo: [" + emailBuscado + "]");

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(emailBuscado);

        if (usuarioOpt.isEmpty()) {
            System.out.println(">>> [AUTH ERROR] No se encontró ningún usuario con el correo: [" + emailBuscado + "]");
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        Usuario usuario = usuarioOpt.get();
        System.out.println(">>> [AUTH] Usuario encontrado en BD con ID: " + usuario.getIdUsuario());
        System.out.println(">>> [AUTH] Estado activo: " + usuario.getActivo());
        System.out.println(">>> [AUTH] Hash/Password en BD: " + usuario.getPassword());

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            System.out.println(">>> [AUTH ERROR] El usuario está inactivo (activo != true)");
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        if (!coincidePassword(request.getPassword(), usuario)) {
            System.out.println(">>> [AUTH ERROR] La contraseña no coincide");
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        String nombreRol = (usuario.getRol() != null && usuario.getRol().getNombre() != null)
                ? usuario.getRol().getNombre()
                : "VOLUNTARIO";

        System.out.println(">>> [AUTH SUCCESS] Login exitoso para rol: " + nombreRol);
        String token = jwtService.generarToken(usuario.getEmail(), nombreRol);

        return new LoginResponse(token, nombreRol);
    }

    private boolean coincidePassword(String rawPassword, Usuario usuario) {
        String storedPassword = usuario.getPassword();
        if (storedPassword == null) {
            return false;
        }

        // 1. Hashes estándar de BCrypt ($2a$, $2b$, $2y$)
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }

        // 2. Hashes con prefijo delegador de Spring ({bcrypt}...)
        if (storedPassword.startsWith("{")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }

        // 3. Si la contraseña en BD está en texto plano, se valida y se migra automáticamente a BCrypt
        if (!storedPassword.equals(rawPassword)) {
            return false;
        }

        usuario.setPassword(passwordEncoder.encode(rawPassword));
        usuarioRepository.save(usuario);
        return true;
    }
}