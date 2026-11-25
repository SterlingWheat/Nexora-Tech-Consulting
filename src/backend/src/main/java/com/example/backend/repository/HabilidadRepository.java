package com.example.backend.repository;

import com.example.backend.model.Habilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HabilidadRepository extends JpaRepository<Habilidad, Long> {

    Optional<Habilidad> findByNombreIgnoreCase(String nombre);
    
    boolean existsByNombreIgnoreCase(String nombre);
}