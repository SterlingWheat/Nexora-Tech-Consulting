package com.example.backend.repository;

import com.example.backend.model.HistorialTurno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialTurnoRepository extends JpaRepository<HistorialTurno, Integer> {
    
    List<HistorialTurno> findByTurnoIdTurnoOrderByFechaRegistroDesc(Integer idTurno);
    
}