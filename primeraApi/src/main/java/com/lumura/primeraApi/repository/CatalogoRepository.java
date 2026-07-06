package com.lumura.primeraApi.repository;

import com.lumura.primeraApi.entity.Catalogo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogoRepository extends JpaRepository<Catalogo, Integer> {
}
