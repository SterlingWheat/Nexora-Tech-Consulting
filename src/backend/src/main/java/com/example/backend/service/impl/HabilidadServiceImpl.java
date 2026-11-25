package com.example.backend.service.impl;

import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.Habilidad;
import com.example.backend.model.Voluntario;
import com.example.backend.repository.HabilidadRepository;
import com.example.backend.repository.VoluntarioRepository;
import com.example.backend.service.HabilidadService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HabilidadServiceImpl implements HabilidadService {

    private final HabilidadRepository habilidadRepository;
    private final VoluntarioRepository voluntarioRepository;

    public HabilidadServiceImpl(HabilidadRepository habilidadRepository, VoluntarioRepository voluntarioRepository) {
        this.habilidadRepository = habilidadRepository;
        this.voluntarioRepository = voluntarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Habilidad> listarHabilidades() {
        List<Habilidad> habilidades = new ArrayList<>(habilidadRepository.findAll());
        habilidades.sort(Comparator.comparing(Habilidad::getNombre));
        return habilidades;
    }

    @Override
    @Transactional
    public Habilidad crearHabilidad(String nombre) {
        String nombreNormalizado = nombre.trim();
        if (habilidadRepository.existsByNombreIgnoreCase(nombreNormalizado)) {
            throw new ConflictException("Ya existe una habilidad con el nombre '" + nombreNormalizado + "'");
        }
        return habilidadRepository.save(Habilidad.builder().nombre(nombreNormalizado).build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Habilidad> obtenerHabilidadesDeVoluntario(Integer idVoluntario) {
        return exigirVoluntario(idVoluntario).getHabilidades().stream()
                .sorted(Comparator.comparing(Habilidad::getNombre))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<Habilidad> asignarHabilidades(Integer idVoluntario, List<Integer> idsHabilidades) {
        Voluntario voluntario = exigirVoluntario(idVoluntario);

        Set<Habilidad> habilidades = new HashSet<>();
        for (Integer idHabilidad : idsHabilidades) {
            habilidades.add(habilidadRepository.findById(idHabilidad)
                    .orElseThrow(() -> new ResourceNotFoundException("Habilidad no encontrada con ID: " + idHabilidad)));
        }

        voluntario.setHabilidades(habilidades);
        voluntarioRepository.save(voluntario);
        return habilidades.stream().sorted(Comparator.comparing(Habilidad::getNombre)).collect(Collectors.toList());
    }

    private Voluntario exigirVoluntario(Integer idVoluntario) {
        return voluntarioRepository.findById(idVoluntario)
                .orElseThrow(() -> new ResourceNotFoundException("Voluntario no encontrado con ID: " + idVoluntario));
    }
}
