package com.example.backend.repository;

import com.example.backend.model.Disponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisponibilidadRepository extends JpaRepository<Disponibilidad, Integer> {

    List<Disponibilidad> findByVoluntarioIdVoluntarioOrderByDiaSemanaAscHoraInicioAsc(Integer idVoluntario);

    void deleteByVoluntarioIdVoluntario(Integer idVoluntario);
}
