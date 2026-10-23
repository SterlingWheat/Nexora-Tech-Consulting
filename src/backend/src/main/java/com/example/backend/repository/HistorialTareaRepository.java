package com.example.backend.repository;

import com.example.backend.model.HistorialTarea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialTareaRepository extends JpaRepository<HistorialTarea, Integer> {
    
    List<HistorialTarea> findByTareaIdTareaOrderByFechaRegistroDesc(Integer idTarea);
    
}