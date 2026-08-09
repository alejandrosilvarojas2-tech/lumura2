package com.lumura.primeraApi.controller;

import com.lumura.primeraApi.entity.Catalogo;
import com.lumura.primeraApi.entity.Usuario;
import com.lumura.primeraApi.repository.CatalogoRepository;
import com.lumura.primeraApi.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final CatalogoRepository catalogoRepository;
    private final UsuarioRepository usuarioRepository;

    public ProductoController(CatalogoRepository catalogoRepository, UsuarioRepository usuarioRepository) {
        this.catalogoRepository = catalogoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public List<Catalogo> listar() {
        List<Catalogo> productos = catalogoRepository.findAll();
        poblarAliados(productos);
        return productos;
    }

    @GetMapping("/page")
    public ResponseEntity<?> listarPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Catalogo> result = catalogoRepository.findAll(pageable);
        poblarAliados(result.getContent());
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
                .map(p -> {
                    poblarAliados(List.of(p));
                    return ResponseEntity.ok(p);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private void poblarAliados(List<Catalogo> productos) {
        Set<Integer> ids = productos.stream()
                .map(Catalogo::getIdAliado)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return;

        Map<Integer, Usuario> aliados = usuarioRepository.findAllById(ids).stream()
                .filter(u -> "ALIADO".equals(u.getRol()))
                .collect(Collectors.toMap(Usuario::getIdUsuario, u -> u));

        for (Catalogo p : productos) {
            if (p.getIdAliado() == null) continue;
            Usuario u = aliados.get(p.getIdAliado());
            if (u == null) continue;
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("nombre_negocio", u.getNombreNegocio());
            info.put("nit", u.getNit());
            info.put("persona_contacto", u.getPersonaContacto());
            info.put("telefono", u.getTelefono());
            info.put("direccion", u.getDireccionUsuario());
            p.setAliado(info);
        }
    }
}
