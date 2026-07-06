package com.lumura.primeraApi.repository;

import com.lumura.primeraApi.entity.Compra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompraRepository extends JpaRepository<Compra, Integer> {
    List<Compra> findByIdUsuarioOrderByFechaPedidoDesc(Integer idUsuario);
    List<Compra> findAllByOrderByFechaPedidoDesc();
}
