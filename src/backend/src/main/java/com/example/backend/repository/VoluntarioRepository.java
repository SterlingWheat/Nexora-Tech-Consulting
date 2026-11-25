package com.example.backend.repository;

import com.example.backend.model.DiaSemana;
import com.example.backend.model.Voluntario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface VoluntarioRepository extends JpaRepository<Voluntario, Integer> {
    boolean existsByDocumentoIgnoreCase(String documento);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByDocumentoIgnoreCaseAndIdVoluntarioNot(String documento, Integer idVoluntario);
    boolean existsByEmailIgnoreCaseAndIdVoluntarioNot(String email, Integer idVoluntario);

    // Permite encontrar al voluntario que corresponde a un ID de usuario para sincronizar el estado
    Optional<Voluntario> findByUsuarioIdUsuario(Integer idUsuario);

    // KAN-27 y HU2: Búsqueda con soporte para nombre, estado, día de disponibilidad y catálogo de habilidades.
    // Se añade LEFT JOIN con habilidades para filtrar por catálogo si se especifica un habilidadId.
    @Query("SELECT DISTINCT v FROM Voluntario v " +
           "LEFT JOIN Disponibilidad d ON d.voluntario = v " +
           "LEFT JOIN v.habilidades h " +
           "WHERE (:patron IS NULL OR LOWER(v.nombre) LIKE :patron " +
           "       OR LOWER(v.apellido) LIKE :patron " +
           "       OR LOWER(v.documento) LIKE :patron) " +
           "AND (:activo IS NULL OR v.activo = :activo) " +
           "AND (:diaSemana IS NULL OR d.diaSemana = :diaSemana) " +
           "AND (:habilidadId IS NULL OR h.id = :habilidadId)")
    Page<Voluntario> buscarVoluntarios(
            @Param("patron") String patron,
            @Param("activo") Boolean activo,
            @Param("diaSemana") DiaSemana diaSemana,
            @Param("habilidadId") Long habilidadId,
            Pageable pageable);
}