package com.example.backend.repository;

import com.example.backend.model.Tarea;
import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;

//@Repository
public interface TareaRepository extends JpaRepository<Tarea, Integer> {
    // JpaRepository ya incluye los métodos necesarios para cumplir con la historia de usuario:
    // save() -> Para crear (criterio 1) y editar (criterio 2)
    // findById() -> Para obtener la tarea y validar su estado antes de editar (criterio 3)
}