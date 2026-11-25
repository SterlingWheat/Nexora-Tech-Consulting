package com.example.backend.service;

import com.example.backend.dto.HabilidadDTO;
import com.example.backend.exception.BusinessValidationException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.Habilidad;
import com.example.backend.repository.HabilidadRepository;
import com.example.backend.service.impl.HabilidadServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HabilidadServiceImplTest {

    @Mock
    private HabilidadRepository habilidadRepository;

    @InjectMocks
    private HabilidadServiceImpl habilidadService;

    private Habilidad habilidad;
    private HabilidadDTO habilidadDTO;

    @BeforeEach
    void setUp() {
        habilidad = Habilidad.builder()
                .id(1L)
                .nombre("Primeros Auxilios")
                .descripcion("Capacidad para brindar atención médica básica")
                .build();

        habilidadDTO = HabilidadDTO.builder()
                .nombre("Primeros Auxilios")
                .descripcion("Capacidad para brindar atención médica básica")
                .build();
    }

    @Test
    void createHabilidad_Success() {
        when(habilidadRepository.existsByNombreIgnoreCase("Primeros Auxilios")).thenReturn(false);
        when(habilidadRepository.save(any(Habilidad.class))).thenReturn(habilidad);

        HabilidadDTO result = habilidadService.createHabilidad(habilidadDTO);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Primeros Auxilios", result.getNombre());
        verify(habilidadRepository, times(1)).save(any(Habilidad.class));
    }

    @Test
    void createHabilidad_NombreDuplicado_ThrowsException() {
        when(habilidadRepository.existsByNombreIgnoreCase("Primeros Auxilios")).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> habilidadService.createHabilidad(habilidadDTO));
        verify(habilidadRepository, never()).save(any(Habilidad.class));
    }

    @Test
    void updateHabilidad_Success() {
        when(habilidadRepository.findById(1L)).thenReturn(Optional.of(habilidad));
        when(habilidadRepository.findByNombreIgnoreCase("Primeros Auxilios")).thenReturn(Optional.empty());
        when(habilidadRepository.save(any(Habilidad.class))).thenReturn(habilidad);

        HabilidadDTO result = habilidadService.updateHabilidad(1L, habilidadDTO);

        assertNotNull(result);
        assertEquals("Primeros Auxilios", result.getNombre());
        verify(habilidadRepository, times(1)).save(habilidad);
    }

    @Test
    void updateHabilidad_NombreDuplicadoEnOtraHabilidad_ThrowsException() {
        Habilidad otraHabilidad = Habilidad.builder().id(2L).nombre("Primeros Auxilios").build();
        
        when(habilidadRepository.findById(1L)).thenReturn(Optional.of(habilidad));
        when(habilidadRepository.findByNombreIgnoreCase("Primeros Auxilios")).thenReturn(Optional.of(otraHabilidad));

        assertThrows(BusinessValidationException.class, () -> habilidadService.updateHabilidad(1L, habilidadDTO));
        verify(habilidadRepository, never()).save(any(Habilidad.class));
    }

    @Test
    void deleteHabilidad_Success() {
        when(habilidadRepository.findById(1L)).thenReturn(Optional.of(habilidad));

        habilidadService.deleteHabilidad(1L);

        verify(habilidadRepository, times(1)).delete(habilidad);
    }

    @Test
    void getHabilidadById_Success() {
        when(habilidadRepository.findById(1L)).thenReturn(Optional.of(habilidad));

        HabilidadDTO result = habilidadService.getHabilidadById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getHabilidadById_NotFound_ThrowsException() {
        when(habilidadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> habilidadService.getHabilidadById(99L));
    }

    @Test
    void getAllHabilidades_Success() {
        when(habilidadRepository.findAll()).thenReturn(List.of(habilidad));

        List<HabilidadDTO> result = habilidadService.getAllHabilidades();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Primeros Auxilios", result.get(0).getNombre());
    }
}