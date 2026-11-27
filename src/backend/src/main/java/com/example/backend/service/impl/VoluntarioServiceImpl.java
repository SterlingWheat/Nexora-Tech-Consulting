package com.example.backend.service.impl;

import com.example.backend.dto.HistorialUnificadoResponseDTO;
import com.example.backend.exception.BusinessValidationException;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.DiaSemana;
import com.example.backend.model.Habilidad;
import com.example.backend.model.RolEntity;
import com.example.backend.model.Usuario;
import com.example.backend.model.Voluntario;
import com.example.backend.notification.AccountEmailNotificationEvent;
import com.example.backend.repository.HabilidadRepository;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Service
public class VoluntarioServiceImpl implements VoluntarioService {

    private final VoluntarioRepository voluntarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    
    // HU2: Repositorio inyectado para validar y asignar habilidades
    private final HabilidadRepository habilidadRepository;

    public VoluntarioServiceImpl(VoluntarioRepository voluntarioRepository,
                                 UsuarioRepository usuarioRepository,
                                 RolRepository rolRepository,
                                 PasswordEncoder passwordEncoder,
                                 ApplicationEventPublisher eventPublisher,
                                 HabilidadRepository habilidadRepository) {
        this.voluntarioRepository = voluntarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.habilidadRepository = habilidadRepository;
    }

    @Override
    @Transactional
    public Voluntario registrarVoluntario(Voluntario voluntario, Boolean crearCuenta, String password, List<Long> habilidadesIds) {
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

        // HU2: Asignación de Habilidades en la creación
        if (habilidadesIds != null && !habilidadesIds.isEmpty()) {
            List<Habilidad> habilidades = habilidadRepository.findAllById(habilidadesIds);
            voluntario.setHabilidades(new HashSet<>(habilidades));
        } else {
            voluntario.setHabilidades(new HashSet<>());
        }

        Voluntario guardado = voluntarioRepository.save(voluntario);

        if (Boolean.TRUE.equals(crearCuenta)) {
            publicarBienvenida(guardado.getUsuario());
        }

        return guardado;
    }

    @Override
    @Transactional
    public Voluntario actualizarVoluntario(Integer idVoluntario, Voluntario detalles, List<Long> habilidadesIds) {
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

        // HU2: Actualización de Habilidades
        if (habilidadesIds != null) {
            List<Habilidad> habilidades = habilidadRepository.findAllById(habilidadesIds);
            existente.setHabilidades(new HashSet<>(habilidades));
        } else {
            existente.getHabilidades().clear();
        }
        
        return voluntarioRepository.save(existente);
    }

    @Override
    @Transactional
    public Voluntario actualizarPerfil(Integer idUsuario, String telefono) {
        Voluntario existente = voluntarioRepository.findByUsuarioIdUsuario(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el perfil de voluntario para el usuario autenticado."));

        Usuario usuario = existente.getUsuario();
        if (usuario == null || !Objects.equals(usuario.getIdUsuario(), idUsuario)) {
            throw new ResourceNotFoundException(
                    "No se encontró la cuenta de acceso del voluntario autenticado.");
        }

        // Nombre, apellido y correo quedan fuera de este autoservicio: solo el teléfono se puede editar
        String telefonoNormalizado = (telefono == null || telefono.isBlank()) ? null : telefono.trim();
        existente.setTelefono(telefonoNormalizado);
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
            throw new BusinessValidationException("La contraseña es obligatoria y debe tener al menos 8 caracteres.");
        }
        if (usuarioRepository.existsByEmailIgnoreCase(voluntario.getEmail())) {
            throw new ConflictException("El correo del voluntario ya está siendo usado por otro usuario del sistema.");
        }
        
        RolEntity rol = rolRepository.findByNombre("VOLUNTARIO")
                .orElseThrow(() -> new BusinessValidationException("El rol VOLUNTARIO no está configurado en la base de datos."));
        
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

    @Override
    @Transactional(readOnly = true)
    public Page<Voluntario> buscarVoluntarios(String texto, Boolean activo, DiaSemana diaSemana, Long habilidadId, Pageable pageable) {
        String textoNormalizado = (texto == null || texto.isBlank()) ? null : texto.trim();
        String patron = textoNormalizado == null ? null : "%" + textoNormalizado.toLowerCase() + "%";
        return voluntarioRepository.buscarVoluntarios(patron, activo, diaSemana, habilidadId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistorialUnificadoResponseDTO> obtenerHistorialUnificado(
            Integer idVoluntario, LocalDate desde, LocalDate hasta) {
        obtenerVoluntarioPorId(idVoluntario);
        List<HistorialUnificadoResponseDTO> historial = new ArrayList<>();
        return historial;
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