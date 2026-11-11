package com.example.backend.repository;

import com.example.backend.model.DiaSemana;
import com.example.backend.model.Disponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalTime;
import java.util.List;

public interface DisponibilidadRepository extends JpaRepository<Disponibilidad, Integer> {

    List<Disponibilidad> findByVoluntarioIdVoluntarioOrderByDiaSemanaAscHoraInicioAsc(Integer idVoluntario);

    void deleteByVoluntarioIdVoluntario(Integer idVoluntario);

    // KAN-31 Criterio 3: ¿el voluntario declaró disponibilidad que cubre por completo esta franja?
    boolean existsByVoluntarioIdVoluntarioAndDiaSemanaAndHoraInicioLessThanEqualAndHoraFinGreaterThanEqual(
            Integer idVoluntario, DiaSemana diaSemana, LocalTime horaInicio, LocalTime horaFin);
}
