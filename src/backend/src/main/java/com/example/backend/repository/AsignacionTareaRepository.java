package com.example.backend.repository;

import com.example.backend.model.AsignacionTarea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AsignacionTareaRepository extends JpaRepository<AsignacionTarea, Integer> {
    
    List<AsignacionTarea> findByTareaIdTarea(Integer idTarea);
    
    List<AsignacionTarea> findByVoluntarioIdVoluntario(Integer idVoluntario);

    boolean existsByTareaIdTareaAndVoluntarioIdVoluntario(Integer idTarea, Integer idVoluntario);
}