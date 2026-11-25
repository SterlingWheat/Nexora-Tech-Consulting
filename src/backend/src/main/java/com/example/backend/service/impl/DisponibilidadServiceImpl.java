package com.example.backend.service.impl;

import com.example.backend.exception.BusinessValidationException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.Disponibilidad;
import com.example.backend.model.DiaSemana;
import com.example.backend.model.Voluntario;
import com.example.backend.repository.DisponibilidadRepository;
import com.example.backend.repository.VoluntarioRepository;
import com.example.backend.service.DisponibilidadService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class DisponibilidadServiceImpl implements DisponibilidadService {

    private final DisponibilidadRepository disponibilidadRepository;
    private final VoluntarioRepository voluntarioRepository;

    public DisponibilidadServiceImpl(DisponibilidadRepository disponibilidadRepository,
                                     VoluntarioRepository voluntarioRepository) {
        this.disponibilidadRepository = disponibilidadRepository;
        this.voluntarioRepository = voluntarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Disponibilidad> obtenerDisponibilidad(Integer idVoluntario) {
        exigirVoluntario(idVoluntario);
        return disponibilidadRepository.findByVoluntarioIdVoluntarioOrderByDiaSemanaAscHoraInicioAsc(idVoluntario);
    }

    @Override
    @Transactional
    public Resultado reemplazarDisponibilidad(Integer idVoluntario, List<Disponibilidad> franjas) {
        Voluntario voluntario = exigirVoluntario(idVoluntario);

        // Criterio 2: validar todo antes de tocar la base de datos
        franjas.forEach(this::validarFranja);

        List<String> advertencias = new ArrayList<>();
        List<Disponibilidad> unificadas = unificar(franjas, advertencias);
        unificadas.forEach(f -> f.setVoluntario(voluntario));

        disponibilidadRepository.deleteByVoluntarioIdVoluntario(idVoluntario);
        disponibilidadRepository.flush();
        List<Disponibilidad> guardadas = disponibilidadRepository.saveAll(unificadas);
        return new Resultado(guardadas, advertencias);
    }

    // RF-23: límite razonable para evitar consultas que expandan un rango de fechas enorme
    private static final long RANGO_MAXIMO_DIAS = 60;

    @Override
    @Transactional(readOnly = true)
    public List<Ocurrencia> obtenerCalendario(LocalDate desde, LocalDate hasta, Integer idVoluntario, Long idHabilidad) {
        if (hasta.isBefore(desde)) {
            throw new BusinessValidationException("La fecha 'hasta' (" + hasta + ") debe ser igual o posterior a 'desde' (" + desde + ")");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) > RANGO_MAXIMO_DIAS) {
            throw new BusinessValidationException("El rango del calendario no puede superar los " + RANGO_MAXIMO_DIAS + " días");
        }
        if (idVoluntario != null) {
            exigirVoluntario(idVoluntario);
        }

        List<Disponibilidad> franjas;
        if (idVoluntario != null) {
            franjas = disponibilidadRepository.findByVoluntarioIdVoluntarioOrderByDiaSemanaAscHoraInicioAsc(idVoluntario);
        } else if (idHabilidad != null) {
            franjas = disponibilidadRepository.findByVoluntarioHabilidadesId(idHabilidad);
        } else {
            franjas = disponibilidadRepository.findAll();
        }

        List<Disponibilidad> activas = franjas.stream()
                .filter(f -> Boolean.TRUE.equals(f.getVoluntario().getActivo()))
                .toList();

        List<Ocurrencia> ocurrencias = new ArrayList<>();
        for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
            DiaSemana dia = DiaSemana.desdeNumero((short) fecha.getDayOfWeek().getValue());
            for (Disponibilidad f : activas) {
                if (f.getDiaSemana() == dia) {
                    Voluntario v = f.getVoluntario();
                    ocurrencias.add(new Ocurrencia(v.getIdVoluntario(), v.getNombre(), v.getApellido(),
                            fecha, dia, f.getHoraInicio(), f.getHoraFin()));
                }
            }
        }

        ocurrencias.sort(Comparator.comparing(Ocurrencia::fecha)
                .thenComparing(Ocurrencia::horaInicio)
                .thenComparing(Ocurrencia::idVoluntario));
        return ocurrencias;
    }

    private Voluntario exigirVoluntario(Integer idVoluntario) {
        return voluntarioRepository.findById(idVoluntario)
                .orElseThrow(() -> new ResourceNotFoundException("Voluntario no encontrado con ID: " + idVoluntario));
    }

    private void validarFranja(Disponibilidad f) {
        if (!f.getHoraFin().isAfter(f.getHoraInicio())) {
            throw new BusinessValidationException("La hora de fin (" + f.getHoraFin() + ") debe ser posterior a la hora de inicio ("
                    + f.getHoraInicio() + ") en la franja del " + f.getDiaSemana());
        }
    }

    /** Criterio 3: une franjas que se superponen o se tocan en el mismo día y avisa de ello. */
    private List<Disponibilidad> unificar(List<Disponibilidad> franjas, List<String> advertencias) {
        Map<DiaSemana, List<Disponibilidad>> porDia = new TreeMap<>();
        for (Disponibilidad f : franjas) {
            porDia.computeIfAbsent(f.getDiaSemana(), d -> new ArrayList<>()).add(f);
        }

        List<Disponibilidad> resultado = new ArrayList<>();
        porDia.forEach((dia, delDia) -> {
            delDia.sort(Comparator.comparing(Disponibilidad::getHoraInicio));
            LocalTime inicio = delDia.get(0).getHoraInicio();
            LocalTime fin = delDia.get(0).getHoraFin();
            boolean huboUnificacion = false;

            for (int i = 1; i < delDia.size(); i++) {
                Disponibilidad actual = delDia.get(i);
                if (!actual.getHoraInicio().isAfter(fin)) {
                    if (actual.getHoraFin().isAfter(fin)) {
                        fin = actual.getHoraFin();
                    }
                    huboUnificacion = true;
                } else {
                    resultado.add(nueva(dia, inicio, fin));
                    inicio = actual.getHoraInicio();
                    fin = actual.getHoraFin();
                }
            }
            resultado.add(nueva(dia, inicio, fin));

            if (huboUnificacion) {
                advertencias.add("Se unificaron franjas superpuestas del " + dia);
            }
        });
        return resultado;
    }

    private Disponibilidad nueva(DiaSemana dia, LocalTime inicio, LocalTime fin) {
        return Disponibilidad.builder().diaSemana(dia).horaInicio(inicio).horaFin(fin).build();
    }
}
