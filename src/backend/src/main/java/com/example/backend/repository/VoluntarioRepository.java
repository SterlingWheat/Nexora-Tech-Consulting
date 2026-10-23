package com.example.backend.repository;

import com.example.backend.model.Voluntario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VoluntarioRepository extends JpaRepository<Voluntario, Integer> {
    boolean existsByDocumentoIgnoreCase(String documento);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByDocumentoIgnoreCaseAndIdVoluntarioNot(String documento, Integer idVoluntario);
    boolean existsByEmailIgnoreCaseAndIdVoluntarioNot(String email, Integer idVoluntario);
    
    // Solución Error 2: Permite encontrar al voluntario que corresponde a un ID de usuario para sincronizar el estado
    Optional<Voluntario> findByUsuarioIdUsuario(Integer idUsuario);
}