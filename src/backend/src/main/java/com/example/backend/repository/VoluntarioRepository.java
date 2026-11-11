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

    // Solución Error 2: Permite encontrar al voluntario que corresponde a un ID de usuario para sincronizar el estado
    Optional<Voluntario> findByUsuarioIdUsuario(Integer idUsuario);

    // KAN-27 Criterios 1 y 2: búsqueda por nombre/apellido/documento, filtro por estado y por día de disponibilidad
    // El patrón "%texto%" se construye en el servicio (no con CONCAT en el HQL): un parámetro nulo dentro de
    // CONCAT hace que Hibernate/el driver de Postgres no puedan inferir su tipo y Postgres lo trata como
    // bytea ("function lower(bytea) does not exist"), que es justo el caso por defecto sin texto de búsqueda.
    @Query("SELECT DISTINCT v FROM Voluntario v " +
           "LEFT JOIN Disponibilidad d ON d.voluntario = v " +
           "WHERE (:patron IS NULL OR LOWER(v.nombre) LIKE :patron " +
           "       OR LOWER(v.apellido) LIKE :patron " +
           "       OR LOWER(v.documento) LIKE :patron) " +
           "AND (:activo IS NULL OR v.activo = :activo) " +
           "AND (:diaSemana IS NULL OR d.diaSemana = :diaSemana)")
    Page<Voluntario> buscarVoluntarios(
            @Param("patron") String patron,
            @Param("activo") Boolean activo,
            @Param("diaSemana") DiaSemana diaSemana,
            Pageable pageable);
}