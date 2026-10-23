package com.example.backend.repository;

import com.example.backend.model.Turno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TurnoRepository extends JpaRepository<Turno, Integer> {

    List<Turno> findAllByOrderByFechaAscHoraInicioAsc();
}
