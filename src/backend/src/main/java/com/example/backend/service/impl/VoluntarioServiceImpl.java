package com.example.backend.service.impl;

import com.example.backend.exception.BusinessValidationException;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.RolEntity;
import com.example.backend.model.Usuario;
import com.example.backend.model.Voluntario;
import com.example.backend.repository.RolRepository;
import com.example.backend.repository.UsuarioRepository;
import com.example.backend.repository.VoluntarioRepository;
import com.example.backend.service.VoluntarioService;
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

    public VoluntarioServiceImpl(VoluntarioRepository voluntarioRepository,
                                 UsuarioRepository usuarioRepository,
                                 RolRepository rolRepository,
                                 PasswordEncoder passwordEncoder) {
        this.voluntarioRepository = voluntarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
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

        return voluntarioRepository.save(voluntario);
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
        return voluntarioRepository.save(voluntario);
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