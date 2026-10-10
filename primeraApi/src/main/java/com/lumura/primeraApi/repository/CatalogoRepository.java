package com.lumura.primeraApi.repository;

import com.lumura.primeraApi.entity.Catalogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CatalogoRepository extends JpaRepository<Catalogo, Integer> {
    Optional<Catalogo> findByArticuloIgnoreCase(String articulo);
    List<Catalogo> findByIdAliado(Integer idAliado);

    // Decremento atómico con guarda de stock: solo decrementa si hay disponibilidad y
    // devuelve cuántas filas afectó (0 si no había stock suficiente). Evita carreras.
    @Modifying
    @Query("update Catalogo c set c.stock = c.stock - :cantidad where c.idCatalogo = :id and c.stock >= :cantidad")
    int descontarStock(@Param("id") Integer id, @Param("cantidad") int cantidad);
}
