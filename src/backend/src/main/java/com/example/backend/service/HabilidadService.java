package com.example.backend.service;

import com.example.backend.dto.HabilidadDTO;

import java.util.List;

public interface HabilidadService {
    
    HabilidadDTO createHabilidad(HabilidadDTO habilidadDTO);
    
    HabilidadDTO updateHabilidad(Long id, HabilidadDTO habilidadDTO);
    
    void deleteHabilidad(Long id);
    
    HabilidadDTO getHabilidadById(Long id);
    
    List<HabilidadDTO> getAllHabilidades();
}