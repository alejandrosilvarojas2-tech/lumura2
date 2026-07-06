package com.lumura.primeraApi.controller;

import com.lumura.primeraApi.entity.Catalogo;
import com.lumura.primeraApi.repository.CatalogoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final CatalogoRepository catalogoRepository;

    public ProductoController(CatalogoRepository catalogoRepository) {
        this.catalogoRepository = catalogoRepository;
    }

    @GetMapping
    public List<Catalogo> listar() {
        return catalogoRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Catalogo> detalle(@PathVariable Integer id) {
        return catalogoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
