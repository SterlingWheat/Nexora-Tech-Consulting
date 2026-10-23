package com.example.backend.service;

import com.example.backend.exception.BusinessValidationException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.DiaSemana;
import com.example.backend.model.Disponibilidad;
import com.example.backend.model.Voluntario;
import com.example.backend.repository.DisponibilidadRepository;
import com.example.backend.repository.VoluntarioRepository;
import com.example.backend.service.impl.DisponibilidadServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisponibilidadServiceImplTest {

    @Mock
    private DisponibilidadRepository disponibilidadRepository;
    @Mock
    private VoluntarioRepository voluntarioRepository;

    private DisponibilidadServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DisponibilidadServiceImpl(disponibilidadRepository, voluntarioRepository);
    }

    private static Disponibilidad franja(DiaSemana dia, String inicio, String fin) {
        return Disponibilidad.builder().diaSemana(dia)
                .horaInicio(LocalTime.parse(inicio)).horaFin(LocalTime.parse(fin)).build();
    }

    private void voluntarioExiste() {
        when(voluntarioRepository.findById(1)).thenReturn(Optional.of(Voluntario.builder().idVoluntario(1).build()));
    }

    private void guardarDevuelveLoRecibido() {
        when(disponibilidadRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));
    }

    // Criterio 1
    @Test
    void reemplazar_conFranjasValidas_reemplazaYAsociaAlVoluntario() {
        voluntarioExiste();
        guardarDevuelveLoRecibido();

        var resultado = service.reemplazarDisponibilidad(1, List.of(
                franja(DiaSemana.LUNES, "08:00", "12:00"),
                franja(DiaSemana.MARTES, "14:00", "18:00")));

        assertEquals(2, resultado.franjas().size());
        assertTrue(resultado.advertencias().isEmpty());
        resultado.franjas().forEach(f -> assertEquals(1, f.getVoluntario().getIdVoluntario()));
        var orden = inOrder(disponibilidadRepository);
        orden.verify(disponibilidadRepository).deleteByVoluntarioIdVoluntario(1);
        orden.verify(disponibilidadRepository).saveAll(any());
    }

    // Criterio 2
    @Test
    void reemplazar_horaFinAnteriorAInicio_seRechazaSinTocarLaBD() {
        voluntarioExiste();

        assertThrows(BusinessValidationException.class, () -> service.reemplazarDisponibilidad(1, List.of(
                franja(DiaSemana.LUNES, "08:00", "12:00"),
                franja(DiaSemana.MARTES, "18:00", "14:00"))));

        verify(disponibilidadRepository, never()).deleteByVoluntarioIdVoluntario(any());
        verify(disponibilidadRepository, never()).saveAll(any());
    }

    // Criterio 2 (borde)
    @Test
    void reemplazar_horaFinIgualAInicio_seRechaza() {
        voluntarioExiste();

        assertThrows(BusinessValidationException.class, () -> service.reemplazarDisponibilidad(1, List.of(
                franja(DiaSemana.LUNES, "08:00", "08:00"))));
    }

    // Criterio 3
    @Test
    void reemplazar_franjasSuperpuestasDelMismoDia_seUnificanYAvisa() {
        voluntarioExiste();
        guardarDevuelveLoRecibido();

        var resultado = service.reemplazarDisponibilidad(1, List.of(
                franja(DiaSemana.LUNES, "10:00", "14:00"),
                franja(DiaSemana.LUNES, "08:00", "11:00")));

        assertEquals(1, resultado.franjas().size());
        assertEquals(LocalTime.of(8, 0), resultado.franjas().get(0).getHoraInicio());
        assertEquals(LocalTime.of(14, 0), resultado.franjas().get(0).getHoraFin());
        assertEquals(1, resultado.advertencias().size());
        assertTrue(resultado.advertencias().get(0).contains("LUNES"));
    }

    @Test
    void reemplazar_franjaContenidaEnOtra_conservaLaMayor() {
        voluntarioExiste();
        guardarDevuelveLoRecibido();

        var resultado = service.reemplazarDisponibilidad(1, List.of(
                franja(DiaSemana.JUEVES, "08:00", "18:00"),
                franja(DiaSemana.JUEVES, "10:00", "12:00")));

        assertEquals(1, resultado.franjas().size());
        assertEquals(LocalTime.of(18, 0), resultado.franjas().get(0).getHoraFin());
    }

    @Test
    void reemplazar_franjasQueSeTocan_seUnifican() {
        voluntarioExiste();
        guardarDevuelveLoRecibido();

        var resultado = service.reemplazarDisponibilidad(1, List.of(
                franja(DiaSemana.LUNES, "08:00", "10:00"),
                franja(DiaSemana.LUNES, "10:00", "12:00")));

        assertEquals(1, resultado.franjas().size());
        assertEquals(LocalTime.of(12, 0), resultado.franjas().get(0).getHoraFin());
    }

    @Test
    void reemplazar_franjasSeparadasODeDiasDistintos_noSeUnifican() {
        voluntarioExiste();
        guardarDevuelveLoRecibido();

        var resultado = service.reemplazarDisponibilidad(1, List.of(
                franja(DiaSemana.LUNES, "08:00", "10:00"),
                franja(DiaSemana.LUNES, "11:00", "12:00"),
                franja(DiaSemana.MARTES, "08:00", "10:00")));

        assertEquals(3, resultado.franjas().size());
        assertTrue(resultado.advertencias().isEmpty());
    }

    @Test
    void reemplazar_listaVacia_borraLaDisponibilidad() {
        voluntarioExiste();
        guardarDevuelveLoRecibido();

        var resultado = service.reemplazarDisponibilidad(1, List.of());

        assertTrue(resultado.franjas().isEmpty());
        verify(disponibilidadRepository).deleteByVoluntarioIdVoluntario(1);
    }

    @Test
    void reemplazar_voluntarioInexistente_lanzaNoEncontrado() {
        when(voluntarioRepository.findById(9)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.reemplazarDisponibilidad(9, List.of()));
        verify(disponibilidadRepository, never()).saveAll(any());
    }

    @Test
    void obtener_devuelveFranjasOrdenadasDelRepositorio() {
        voluntarioExiste();
        var f = franja(DiaSemana.LUNES, "08:00", "09:00");
        when(disponibilidadRepository.findByVoluntarioIdVoluntarioOrderByDiaSemanaAscHoraInicioAsc(1)).thenReturn(List.of(f));

        assertEquals(List.of(f), service.obtenerDisponibilidad(1));
    }

    @Test
    void obtener_voluntarioInexistente_lanzaNoEncontrado() {
        when(voluntarioRepository.findById(9)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerDisponibilidad(9));
    }
}
