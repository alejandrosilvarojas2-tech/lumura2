package com.lumura.primeraApi.repository;

import com.lumura.primeraApi.entity.Favorito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoritoRepository extends JpaRepository<Favorito, Integer> {

    List<Favorito> findByIdUsuarioOrderByFechaAgregadoDesc(Integer idUsuario);

    Optional<Favorito> findByIdUsuarioAndIdCatalogo(Integer idUsuario, Integer idCatalogo);

    void deleteByIdUsuarioAndIdCatalogo(Integer idUsuario, Integer idCatalogo);
}
