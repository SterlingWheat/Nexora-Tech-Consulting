package com.example.backend.repository;

import com.example.backend.model.PasswordResetToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    boolean existsByTokenHashAndFechaUsoIsNullAndFechaExpiracionAfter(
            String tokenHash,
            LocalDateTime now);

    @Modifying
    @Query("""
            update PasswordResetToken token
            set token.fechaUso = :now
            where token.usuario.idUsuario = :userId and token.fechaUso is null
            """)
    void invalidateUnusedTokens(@Param("userId") Integer userId, @Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from PasswordResetToken token where token.tokenHash = :tokenHash")
    Optional<PasswordResetToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
