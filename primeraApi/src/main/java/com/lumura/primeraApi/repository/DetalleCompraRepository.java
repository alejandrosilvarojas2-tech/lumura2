package com.lumura.primeraApi.repository;

import com.lumura.primeraApi.entity.DetalleCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DetalleCompraRepository extends JpaRepository<DetalleCompra, Integer> {

    List<DetalleCompra> findByIdCompra(Integer idCompra);

    List<DetalleCompra> findByIdCompraIn(List<Integer> idsCompras);

    // Ventas de un aliado: líneas de pedido cuyo producto pertenece al aliado
    @Query("SELECT d FROM DetalleCompra d WHERE d.idCatalogo IN " +
           "(SELECT c.idCatalogo FROM Catalogo c WHERE c.idAliado = :idAliado) " +
           "ORDER BY d.idDetalle DESC")
    List<DetalleCompra> findVentasDeAliado(@Param("idAliado") Integer idAliado);
}

