package com.lumura.primeraApi.controller;

import com.lumura.primeraApi.entity.Catalogo;
import com.lumura.primeraApi.repository.CatalogoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @GetMapping("/page")
    public ResponseEntity<?> listarPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Catalogo> result = catalogoRepository.findAll(pageable);
        return ResponseEntity.ok(Map.of(
            "content", result.getContent(),
            "totalPages", result.getTotalPages(),
            "totalElements", result.getTotalElements(),
            "currentPage", result.getNumber()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Catalogo> detalle(@PathVariable Integer id) {
        return catalogoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
