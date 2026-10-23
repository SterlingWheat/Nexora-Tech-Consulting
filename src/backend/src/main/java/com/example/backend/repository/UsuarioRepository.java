package com.example.backend.repository;

import com.example.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    
    // Aunque la base de datos usa la columna "correo"[cite: 3], 
    // en Usuario.java el atributo se llama "email", por lo que 
    // Spring Data JPA sigue usando "findByEmail" correctamente[cite: 1].
    Optional<Usuario> findByEmail(String email);
    
    Boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdUsuarioNot(String email, Integer idUsuario);
}