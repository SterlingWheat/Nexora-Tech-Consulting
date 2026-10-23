package com.example.backend.service;

import com.example.backend.model.Voluntario;
import com.example.backend.repository.VoluntarioRepository;
import com.example.backend.service.impl.VoluntarioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoluntarioServiceImplTest {

    @Mock
    private VoluntarioRepository repository;

    private VoluntarioServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VoluntarioServiceImpl(repository);
    }

    private Voluntario nuevo() {
        Voluntario v = new Voluntario();
        v.setNombre("  Ana ");
        v.setApellido("Quispe");
        v.setDocumento(" 1234567 ");
        v.setEmail(" ANA@Correo.com ");
        v.setTelefono("   ");
        return v;
    }

    // Criterio 1
    @Test
    void registrar_conDatosValidos_quedaActivoYNormalizado() {
        when(repository.save(any(Voluntario.class))).thenAnswer(i -> i.getArgument(0));

        Voluntario guardado = service.registrarVoluntario(nuevo());

        assertTrue(guardado.getActivo());
        assertEquals("Ana", guardado.getNombre());
        assertEquals("1234567", guardado.getDocumento());
        assertEquals("ana@correo.com", guardado.getEmail());
        assertNull(guardado.getTelefono());
    }

    // Criterio 2
    @Test
    void registrar_documentoDuplicado_lanzaConflictoYNoGuarda() {
        when(repository.existsByDocumentoIgnoreCase("1234567")).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.registrarVoluntario(nuevo()));

        assertTrue(ex.getMessage().contains("documento"));
        verify(repository, never()).save(any());
    }

    // Criterio 2
    @Test
    void registrar_correoDuplicado_lanzaConflictoYNoGuarda() {
        when(repository.existsByDocumentoIgnoreCase(any())).thenReturn(false);
        when(repository.existsByEmailIgnoreCase("ana@correo.com")).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.registrarVoluntario(nuevo()));

        assertTrue(ex.getMessage().contains("correo"));
        verify(repository, never()).save(any());
    }

    // Criterio 3
    @Test
    void actualizar_conDatosValidos_guardaCambios() {
        Voluntario existente = Voluntario.builder().idVoluntario(1).nombre("Viejo").apellido("Apellido")
                .documento("111").email("viejo@correo.com").build();
        when(repository.findById(1)).thenReturn(Optional.of(existente));
        when(repository.save(any(Voluntario.class))).thenAnswer(i -> i.getArgument(0));

        Voluntario resultado = service.actualizarVoluntario(1, nuevo());

        assertEquals("Ana", resultado.getNombre());
        assertEquals("ana@correo.com", resultado.getEmail());
        assertEquals(1, resultado.getIdVoluntario());
        assertTrue(resultado.getActivo());
    }

    @Test
    void actualizar_inexistente_lanzaNoEncontrado() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.actualizarVoluntario(99, nuevo()));
        verify(repository, never()).save(any());
    }

    @Test
    void actualizar_conDocumentoDeOtroVoluntario_lanzaConflicto() {
        Voluntario existente = Voluntario.builder().idVoluntario(1).nombre("X").apellido("Y")
                .documento("111").email("x@correo.com").build();
        when(repository.findById(1)).thenReturn(Optional.of(existente));
        when(repository.existsByDocumentoIgnoreCaseAndIdVoluntarioNot("1234567", 1)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.actualizarVoluntario(1, nuevo()));
        verify(repository, never()).save(any());
    }

    @Test
    void actualizar_conCorreoDeOtroVoluntario_lanzaConflicto() {
        Voluntario existente = Voluntario.builder().idVoluntario(1).nombre("X").apellido("Y")
                .documento("111").email("x@correo.com").build();
        when(repository.findById(1)).thenReturn(Optional.of(existente));
        when(repository.existsByEmailIgnoreCaseAndIdVoluntarioNot("ana@correo.com", 1)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.actualizarVoluntario(1, nuevo()));
        verify(repository, never()).save(any());
    }
}
