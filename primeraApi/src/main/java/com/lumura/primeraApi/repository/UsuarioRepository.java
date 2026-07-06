package com.lumura.primeraApi.repository;

import com.lumura.primeraApi.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByCorreoUsuario(String correoUsuario);
}
