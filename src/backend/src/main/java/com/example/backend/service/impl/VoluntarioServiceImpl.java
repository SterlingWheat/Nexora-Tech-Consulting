package com.example.backend.service.impl;

import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.Voluntario;
import com.example.backend.repository.VoluntarioRepository;
import com.example.backend.service.VoluntarioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VoluntarioServiceImpl implements VoluntarioService {

    private final VoluntarioRepository voluntarioRepository;

    public VoluntarioServiceImpl(VoluntarioRepository voluntarioRepository) {
        this.voluntarioRepository = voluntarioRepository;
    }

    @Override
    @Transactional
    public Voluntario registrarVoluntario(Voluntario voluntario) {
        normalizar(voluntario);
        if (voluntarioRepository.existsByDocumentoIgnoreCase(voluntario.getDocumento())) {
            throw new ConflictException("Ya existe un voluntario registrado con el documento " + voluntario.getDocumento());
        }
        if (voluntarioRepository.existsByEmailIgnoreCase(voluntario.getEmail())) {
            throw new ConflictException("Ya existe un voluntario registrado con el correo " + voluntario.getEmail());
        }
        // Criterio 1: el voluntario queda registrado como activo
        voluntario.setActivo(true);
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
        // Criterio 3: la fecha de modificación se completa en @PreUpdate
        return voluntarioRepository.save(existente);
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
