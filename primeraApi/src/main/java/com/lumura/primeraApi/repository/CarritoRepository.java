package com.lumura.primeraApi.repository;

import com.lumura.primeraApi.entity.Carrito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarritoRepository extends JpaRepository<Carrito, Integer> {
    List<Carrito> findByIdUsuario(Integer idUsuario);
    void deleteByIdUsuario(Integer idUsuario);
}
