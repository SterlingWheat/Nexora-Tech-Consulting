package com.example.backend.service.impl;

import com.example.backend.dto.HabilidadDTO;
import com.example.backend.exception.BusinessValidationException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.Habilidad;
import com.example.backend.repository.HabilidadRepository;
import com.example.backend.service.HabilidadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HabilidadServiceImpl implements HabilidadService {

    private final HabilidadRepository habilidadRepository;

    @Override
    @Transactional
    public HabilidadDTO createHabilidad(HabilidadDTO habilidadDTO) {
        if (habilidadRepository.existsByNombreIgnoreCase(habilidadDTO.getNombre().trim())) {
            throw new BusinessValidationException("Ya existe una habilidad con el nombre: " + habilidadDTO.getNombre());
        }

        Habilidad habilidad = Habilidad.builder()
                .nombre(habilidadDTO.getNombre().trim())
                .descripcion(habilidadDTO.getDescripcion())
                .build();

        Habilidad saved = habilidadRepository.save(habilidad);
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public HabilidadDTO updateHabilidad(Long id, HabilidadDTO habilidadDTO) {
        Habilidad habilidad = getEntityById(id);

        // Validar que el nuevo nombre no exista ya en OTRA habilidad
        habilidadRepository.findByNombreIgnoreCase(habilidadDTO.getNombre().trim())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new BusinessValidationException("Ya existe otra habilidad con el nombre: " + habilidadDTO.getNombre());
                    }
                });

        habilidad.setNombre(habilidadDTO.getNombre().trim());
        habilidad.setDescripcion(habilidadDTO.getDescripcion());

        Habilidad updated = habilidadRepository.save(habilidad);
        return mapToDTO(updated);
    }

    @Override
    @Transactional
    public void deleteHabilidad(Long id) {
        Habilidad habilidad = getEntityById(id);
        // Al tener CascadeType.REMOVE / ON DELETE CASCADE en tabla intermedia,
        // esto removerá la habilidad y limpiará sus asociaciones en voluntario_habilidad.
        habilidadRepository.delete(habilidad);
    }

    @Override
    @Transactional(readOnly = true)
    public HabilidadDTO getHabilidadById(Long id) {
        return mapToDTO(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HabilidadDTO> getAllHabilidades() {
        return habilidadRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // Métodos auxiliares
    private Habilidad getEntityById(Long id) {
        return habilidadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Habilidad no encontrada con id: " + id));
    }

    private HabilidadDTO mapToDTO(Habilidad habilidad) {
        return HabilidadDTO.builder()
                .id(habilidad.getId())
                .nombre(habilidad.getNombre())
                .descripcion(habilidad.getDescripcion())
                .build();
    }
}