package com.example.backend.repository;

import com.example.backend.model.HistorialTarea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialTareaRepository extends JpaRepository<HistorialTarea, Integer> {
    
    // HU2: Obtiene los registros ordenados desde el más reciente al más antiguo para la línea de tiempo
    List<HistorialTarea> findByTareaIdTareaOrderByFechaRegistroDesc(Integer idTarea);
    
}