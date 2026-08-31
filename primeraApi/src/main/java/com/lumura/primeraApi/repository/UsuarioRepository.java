package com.lumura.primeraApi.repository;

import com.lumura.primeraApi.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByCorreoUsuario(String correoUsuario);
    Optional<Usuario> findByResetToken(String resetToken);

    @Modifying
    @Query("UPDATE Usuario u SET u.tokenVersion = COALESCE(u.tokenVersion, 0) + 1 WHERE u.idUsuario = :idUsuario")
    int incrementarTokenVersion(@Param("idUsuario") Integer idUsuario);
}
