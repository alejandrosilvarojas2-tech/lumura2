package com.lumura.primeraApi.repository;

import com.lumura.primeraApi.entity.Catalogo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CatalogoRepository extends JpaRepository<Catalogo, Integer> {
    Optional<Catalogo> findByArticuloIgnoreCase(String articulo);
}
