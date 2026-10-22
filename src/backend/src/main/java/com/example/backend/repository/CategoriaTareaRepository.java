package com.example.backend.repository;

import com.example.backend.model.CategoriaTarea;
import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;

//@Repository
public interface CategoriaTareaRepository extends JpaRepository<CategoriaTarea, Integer> {
    // Aquí puedes agregar consultas personalizadas si las necesitas en el futuro, 
    // por ejemplo: Optional<CategoriaTarea> findByNombre(String nombre);
}