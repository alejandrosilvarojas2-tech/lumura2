package com.lumura.primeraApi.repository;

import com.lumura.primeraApi.entity.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface CompraRepository extends JpaRepository<Compra, Integer> {
    List<Compra> findByIdUsuarioOrderByFechaPedidoDesc(Integer idUsuario);
    List<Compra> findAllByOrderByFechaPedidoDesc();

    @Query("SELECT COALESCE(SUM(c.total), 0) FROM Compra c")
    BigDecimal sumTotal();
}
