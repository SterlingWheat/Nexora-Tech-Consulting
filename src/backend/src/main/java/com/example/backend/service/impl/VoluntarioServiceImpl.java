package com.example.backend.service.impl;

import com.example.backend.exception.BusinessValidationException;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.DiaSemana;
import com.example.backend.model.RolEntity;
import com.example.backend.model.Usuario;
import com.example.backend.model.Voluntario;
import com.example.backend.notification.AccountEmailNotificationEvent;
import com.example.backend.repository.RolRepository;
import com.example.backend.repository.UsuarioRepository;
import com.example.backend.repository.VoluntarioRepository;
import com.example.backend.service.VoluntarioService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VoluntarioServiceImpl implements VoluntarioService {

    private final VoluntarioRepository voluntarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    public VoluntarioServiceImpl(VoluntarioRepository voluntarioRepository,
                                 UsuarioRepository usuarioRepository,
                                 RolRepository rolRepository,
                                 PasswordEncoder passwordEncoder,
                                 ApplicationEventPublisher eventPublisher) {
        this.voluntarioRepository = voluntarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Voluntario registrarVoluntario(Voluntario voluntario, Boolean crearCuenta, String password) {
        normalizar(voluntario);

        if (voluntarioRepository.existsByDocumentoIgnoreCase(voluntario.getDocumento())) {
            throw new ConflictException("Ya existe un voluntario registrado con el documento " + voluntario.getDocumento());
        }
        if (voluntarioRepository.existsByEmailIgnoreCase(voluntario.getEmail())) {
            throw new ConflictException("Ya existe un voluntario registrado con el correo " + voluntario.getEmail());
        }

        voluntario.setActivo(true);

        if (Boolean.TRUE.equals(crearCuenta)) {
            crearCuentaAcceso(voluntario, password);
        }

        Voluntario guardado = voluntarioRepository.save(voluntario);

        if (Boolean.TRUE.equals(crearCuenta)) {
            publicarBienvenida(guardado.getUsuario());
        }

        return guardado;
    }

    @Override
    @Transactional
    public Voluntario actualizarVoluntario(Integer idVoluntario, Voluntario detalles) {
        Voluntario existente = obtenerVoluntarioPorId(idVoluntario);
        normalizar(detalles);

        if (voluntarioRepository.existsByDocumentoIgnoreCaseAndIdVoluntarioNot(detalles.getDocumento(), idVoluntario)) {
            throw new ConflictException("Ya existe un voluntario registrado con el documento " + detalles.getDocumento());
        }
        if (voluntarioRepository.existsByEmailIgnoreCaseAndIdVoluntarioNot(detalles.getEmail(), idVoluntario)) {
            throw new ConflictException("Ya existe un voluntario registrado con el correo " + detalles.getEmail());
        }
        
        existente.setNombre(detalles.getNombre());
        existente.setApellido(detalles.getApellido());
        existente.setDocumento(detalles.getDocumento());
        existente.setEmail(detalles.getEmail());
        existente.setTelefono(detalles.getTelefono());
        
        return voluntarioRepository.save(existente);
    }

    @Override
    @Transactional
    public Voluntario habilitarAcceso(Integer idVoluntario, String password) {
        Voluntario voluntario = obtenerVoluntarioPorId(idVoluntario);
        
        if (voluntario.getUsuario() != null) {
            throw new ConflictException("Este voluntario ya tiene una cuenta de acceso al sistema.");
        }
        
        crearCuentaAcceso(voluntario, password);
        Voluntario actualizado = voluntarioRepository.save(voluntario);
        publicarBienvenida(actualizado.getUsuario());

        return actualizado;
    }

    // Soluci n Error 2: Mantiene la consistencia de acceso entre el Voluntario y su Usuario si lo posee
    @Override
    @Transactional
    public Voluntario cambiarEstado(Integer idVoluntario, boolean activo) {
        Voluntario voluntario = obtenerVoluntarioPorId(idVoluntario);
        voluntario.setActivo(activo);
        
        if (voluntario.getUsuario() != null) {
            Usuario usuario = voluntario.getUsuario();
            usuario.setActivo(activo);
            usuarioRepository.save(usuario);

            eventPublisher.publishEvent(new AccountEmailNotificationEvent(
                    AccountEmailNotificationEvent.Kind.ACCOUNT_CHANGE,
                    usuario.getEmail(),
                    usuario.getNombre(),
                    activo
                            ? "El acceso a tu cuenta fue activado por un administrador."
                            : "El acceso a tu cuenta fue desactivado por un administrador."));
        }
        
        return voluntarioRepository.save(voluntario);
    }

    private void publicarBienvenida(Usuario usuario) {
        eventPublisher.publishEvent(new AccountEmailNotificationEvent(
                AccountEmailNotificationEvent.Kind.WELCOME,
                usuario.getEmail(),
                usuario.getNombre(),
                "Tu cuenta de acceso ha sido creada."));
    }

    private void crearCuentaAcceso(Voluntario voluntario, String password) {
        if (password == null || password.trim().length() < 8) {
            throw new BusinessValidationException("La contrase a es obligatoria y debe tener al menos 8 caracteres.");
        }
        if (usuarioRepository.existsByEmailIgnoreCase(voluntario.getEmail())) {
            throw new ConflictException("El correo del voluntario ya est  siendo usado por otro usuario del sistema.");
        }
        
        RolEntity rol = rolRepository.findByNombre("VOLUNTARIO")
                .orElseThrow(() -> new BusinessValidationException("El rol VOLUNTARIO no est  configurado en la base de datos."));
        
        Usuario nuevoUsuario = Usuario.builder()
                .nombre(voluntario.getNombre() + " " + voluntario.getApellido())
                .email(voluntario.getEmail())
                .password(passwordEncoder.encode(password))
                .rol(rol)
                .estado("ACTIVO")
                .build();
        
        nuevoUsuario = usuarioRepository.save(nuevoUsuario);
        voluntario.setUsuario(nuevoUsuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Voluntario> listarVoluntarios() {
        return voluntarioRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Voluntario obtenerVoluntarioPorId(Integer idVoluntario) {
        return voluntarioRepository.findById(idVoluntario)
                .orElseThrow(() -> new ResourceNotFoundException("Voluntario no encontrado con ID: " + idVoluntario));
    }

    // KAN-27 Criterios 1, 2 y 3: delega el filtrado y la paginación a la base de datos
    @Override
    @Transactional(readOnly = true)
    public Page<Voluntario> buscarVoluntarios(String texto, Boolean activo, DiaSemana diaSemana, Pageable pageable) {
        String textoNormalizado = (texto == null || texto.isBlank()) ? null : texto.trim();
        // El patrón LIKE se arma aquí (no con CONCAT en el HQL) para evitar el bug de Postgres/Hibernate
        // con parámetros nulos dentro de CONCAT (ver comentario en VoluntarioRepository.buscarVoluntarios).
        String patron = textoNormalizado == null ? null : "%" + textoNormalizado.toLowerCase() + "%";
        return voluntarioRepository.buscarVoluntarios(patron, activo, diaSemana, pageable);
    }

    private void normalizar(Voluntario v) {
        v.setNombre(v.getNombre().trim());
        v.setApellido(v.getApellido().trim());
        v.setDocumento(v.getDocumento().trim());
        v.setEmail(v.getEmail().trim().toLowerCase());
        if (v.getTelefono() != null) {
            String tel = v.getTelefono().trim();
            v.setTelefono(tel.isEmpty() ? null : tel);
        }
    }
}