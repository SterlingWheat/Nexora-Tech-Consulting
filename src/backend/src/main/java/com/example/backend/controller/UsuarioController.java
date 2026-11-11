package com.example.backend.controller;

import com.example.backend.dto.UsuarioCreateRequest;
import com.example.backend.dto.UsuarioResponse;
import com.example.backend.dto.UsuarioUpdateRequest;
import com.example.backend.model.RolEntity;
import com.example.backend.model.Usuario;
import com.example.backend.notification.AccountEmailNotificationEvent;
import com.example.backend.repository.RolRepository;
import com.example.backend.repository.UsuarioRepository;
import com.example.backend.repository.VoluntarioRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final VoluntarioRepository voluntarioRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UsuarioController(UsuarioRepository usuarioRepository,
                              RolRepository rolRepository,
                              PasswordEncoder passwordEncoder,
                             VoluntarioRepository voluntarioRepository,
                             ApplicationEventPublisher eventPublisher) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.voluntarioRepository = voluntarioRepository;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping
    public ResponseEntity<?> crearUsuario(@RequestBody UsuarioCreateRequest request) {
        if (request.email() == null || request.email().isBlank()
                || request.password() == null || request.password().isBlank()
                || request.password().length() < 8
                || request.nombre() == null || request.nombre().isBlank()
                || request.rol() == null) {
            return ResponseEntity.badRequest().body("Datos de usuario inv lidos.");
        }

        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            return ResponseEntity.status(409).body("El correo ya est  registrado.");
        }

        String nombreRol = request.rol().name();
        RolEntity rolEntity = rolRepository.findByNombre(nombreRol)
                .orElseThrow(() -> new RuntimeException("El rol " + nombreRol + " no existe en la base de datos."));

        Usuario usuario = Usuario.builder()
                .nombre(request.nombre().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .rol(rolEntity)
                .estado("ACTIVO")
                .build();

        Usuario guardado = usuarioRepository.save(usuario);

        eventPublisher.publishEvent(new AccountEmailNotificationEvent(
                AccountEmailNotificationEvent.Kind.WELCOME,
                guardado.getEmail(),
                guardado.getNombre(),
                "Tu cuenta de acceso ha sido creada."));

        return ResponseEntity.ok(UsuarioResponse.from(guardado));
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listarUsuarios(
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) Boolean activo) {
        
        List<Usuario> usuarios = usuarioRepository.findAll();

        if (rol != null && !rol.isEmpty()) {
            usuarios = usuarios.stream()
                    .filter(u -> u.getRol().getNombre().equalsIgnoreCase(rol))
                    .toList();
        }
        if (activo != null) {
            usuarios = usuarios.stream()
                    .filter(u -> u.getActivo().equals(activo))
                    .toList();
        }

        return ResponseEntity.ok(usuarios.stream().map(UsuarioResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtenerUsuario(@PathVariable Integer id) {
        return usuarioRepository.findById(id)
                .map(UsuarioResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarUsuario(
            @PathVariable Integer id,
            @RequestBody UsuarioUpdateRequest request) {
        
        if (request.nombre() == null || request.nombre().isBlank()
                || request.email() == null || request.email().isBlank()
                || (request.password() != null && !request.password().isBlank()
                        && request.password().length() < 8)
                || request.rol() == null) {
            return ResponseEntity.badRequest().body("Datos de usuario inv lidos.");
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmailIgnoreCaseAndIdUsuarioNot(email, id)) {
            return ResponseEntity.status(409).body("El correo ya est  registrado.");
        }

        String nombreRol = request.rol().name();
        RolEntity rolEntity = rolRepository.findByNombre(nombreRol)
                .orElseThrow(() -> new RuntimeException("El rol " + nombreRol + " no existe en la base de datos."));

        Usuario usuario = usuarioOpt.get();
        String emailAnterior = usuario.getEmail();
        String nombreAnterior = usuario.getNombre();
        String rolAnterior = usuario.getRol() == null ? null : usuario.getRol().getNombre();
        
        boolean cambioContrasena = request.password() != null && !request.password().isBlank();
        List<String> cambios = new ArrayList<>();

        if (!Objects.equals(nombreAnterior, request.nombre().trim())) {
            cambios.add("Se actualiz  el nombre asociado a tu cuenta.");
        }
        if (emailAnterior == null || !emailAnterior.equalsIgnoreCase(email)) {
            cambios.add("Se actualiz  el correo asociado a tu cuenta.");
        }
        if (rolAnterior == null || !rolAnterior.equals(nombreRol)) {
            cambios.add("Se actualiz  el rol de tu cuenta.");
        }

        usuario.setNombre(request.nombre().trim());
        usuario.setEmail(email);
        usuario.setRol(rolEntity);

        if (cambioContrasena) {
            usuario.setPassword(passwordEncoder.encode(request.password()));
        }

        Usuario actualizado = usuarioRepository.save(usuario);

        if (cambioContrasena) {
            eventPublisher.publishEvent(new AccountEmailNotificationEvent(
                    AccountEmailNotificationEvent.Kind.PASSWORD_RESET,
                    actualizado.getEmail(),
                    actualizado.getNombre(),
                    "La contrase a de tu cuenta fue actualizada por un administrador."));
        } else if (!cambios.isEmpty()) {
            eventPublisher.publishEvent(new AccountEmailNotificationEvent(
                    AccountEmailNotificationEvent.Kind.ACCOUNT_CHANGE,
                    actualizado.getEmail(),
                    actualizado.getNombre(),
                    String.join(" ", cambios)));
        }

        return ResponseEntity.ok(UsuarioResponse.from(actualizado));
    }

    // Soluci n Error 2: Sincronizar desactivaci n de cuenta de usuario con la entidad Voluntario
    @PutMapping("/{id}/desactivar")
    public ResponseEntity<?> desactivarUsuario(@PathVariable Integer id) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Usuario usuario = usuarioOpt.get();
        usuario.setActivo(false);
        usuarioRepository.save(usuario);

        // Si el usuario es de un voluntario, desactivar tambi n al voluntario
        voluntarioRepository.findByUsuarioIdUsuario(id).ifPresent(vol -> {
            vol.setActivo(false);
            voluntarioRepository.save(vol);
        });

        eventPublisher.publishEvent(new AccountEmailNotificationEvent(
                AccountEmailNotificationEvent.Kind.ACCOUNT_CHANGE,
                usuario.getEmail(),
                usuario.getNombre(),
                "El acceso a tu cuenta fue desactivado por un administrador."));

        return ResponseEntity.ok("Usuario desactivado correctamente.");
    }
    
    // Soluci n Error 2: Nuevo endpoint para reactivar cuentas previamente desactivadas
    @PutMapping("/{id}/activar")
    public ResponseEntity<?> activarUsuario(@PathVariable Integer id) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Usuario usuario = usuarioOpt.get();
        usuario.setActivo(true);
        usuarioRepository.save(usuario);

        // Si el usuario es de un voluntario, activar tambi n al voluntario
        voluntarioRepository.findByUsuarioIdUsuario(id).ifPresent(vol -> {
            vol.setActivo(true);
            voluntarioRepository.save(vol);
        });

        eventPublisher.publishEvent(new AccountEmailNotificationEvent(
                AccountEmailNotificationEvent.Kind.ACCOUNT_CHANGE,
                usuario.getEmail(),
                usuario.getNombre(),
                "El acceso a tu cuenta fue activado por un administrador."));

        return ResponseEntity.ok("Usuario activado correctamente.");
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}