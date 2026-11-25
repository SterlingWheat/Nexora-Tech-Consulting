package com.example.backend.repository;

import com.example.backend.model.Habilidad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabilidadRepository extends JpaRepository<Habilidad, Integer> {
    boolean existsByNombreIgnoreCase(String nombre);
}
