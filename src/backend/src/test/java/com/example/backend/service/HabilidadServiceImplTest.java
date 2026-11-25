package com.example.backend.service;

import com.example.backend.exception.ConflictException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.Habilidad;
import com.example.backend.model.Voluntario;
import com.example.backend.repository.HabilidadRepository;
import com.example.backend.repository.VoluntarioRepository;
import com.example.backend.service.impl.HabilidadServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HabilidadServiceImplTest {

    @Mock
    private HabilidadRepository habilidadRepository;
    @Mock
    private VoluntarioRepository voluntarioRepository;

    private HabilidadServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HabilidadServiceImpl(habilidadRepository, voluntarioRepository);
    }

    private static Habilidad habilidad(Integer id, String nombre) {
        return Habilidad.builder().idHabilidad(id).nombre(nombre).build();
    }

    @Test
    void listarHabilidades_devuelveOrdenadasPorNombre() {
        when(habilidadRepository.findAll()).thenReturn(List.of(habilidad(2, "Traducción"), habilidad(1, "Cocina")));

        List<Habilidad> resultado = service.listarHabilidades();

        assertEquals(List.of("Cocina", "Traducción"), resultado.stream().map(Habilidad::getNombre).toList());
    }

    @Test
    void crearHabilidad_nombreNuevo_seGuarda() {
        when(habilidadRepository.existsByNombreIgnoreCase("Logística")).thenReturn(false);
        when(habilidadRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Habilidad creada = service.crearHabilidad("  Logística  ");

        assertEquals("Logística", creada.getNombre());
    }

    @Test
    void crearHabilidad_nombreDuplicado_lanzaConflicto() {
        when(habilidadRepository.existsByNombreIgnoreCase("Cocina")).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.crearHabilidad("Cocina"));
        verify(habilidadRepository, never()).save(any());
    }

    @Test
    void asignarHabilidades_voluntarioYHabilidadesExisten_lasAsigna() {
        Voluntario voluntario = Voluntario.builder().idVoluntario(1).build();
        when(voluntarioRepository.findById(1)).thenReturn(Optional.of(voluntario));
        when(habilidadRepository.findById(10)).thenReturn(Optional.of(habilidad(10, "Cocina")));
        when(habilidadRepository.findById(20)).thenReturn(Optional.of(habilidad(20, "Logística")));

        List<Habilidad> resultado = service.asignarHabilidades(1, List.of(10, 20));

        assertEquals(2, resultado.size());
        assertEquals(Set.of("Cocina", "Logística"), Set.copyOf(resultado.stream().map(Habilidad::getNombre).toList()));
        assertEquals(2, voluntario.getHabilidades().size());
        verify(voluntarioRepository).save(voluntario);
    }

    @Test
    void asignarHabilidades_voluntarioInexistente_lanzaNoEncontrado() {
        when(voluntarioRepository.findById(9)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.asignarHabilidades(9, List.of(10)));
        verify(habilidadRepository, never()).findById(any());
    }

    @Test
    void asignarHabilidades_habilidadInexistente_lanzaNoEncontrado() {
        when(voluntarioRepository.findById(1)).thenReturn(Optional.of(Voluntario.builder().idVoluntario(1).build()));
        when(habilidadRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.asignarHabilidades(1, List.of(99)));
        verify(voluntarioRepository, never()).save(any());
    }

    @Test
    void obtenerHabilidadesDeVoluntario_devuelveLasAsignadasOrdenadas() {
        Voluntario voluntario = Voluntario.builder().idVoluntario(1)
                .habilidades(Set.of(habilidad(2, "Traducción"), habilidad(1, "Cocina"))).build();
        when(voluntarioRepository.findById(1)).thenReturn(Optional.of(voluntario));

        List<Habilidad> resultado = service.obtenerHabilidadesDeVoluntario(1);

        assertEquals(List.of("Cocina", "Traducción"), resultado.stream().map(Habilidad::getNombre).toList());
    }
}
