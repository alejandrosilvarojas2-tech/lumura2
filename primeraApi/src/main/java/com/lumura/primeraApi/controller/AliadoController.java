package com.lumura.primeraApi.controller;

import com.lumura.primeraApi.entity.Catalogo;
import com.lumura.primeraApi.repository.CatalogoRepository;
import com.lumura.primeraApi.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/aliado")
public class AliadoController {

    private static final Logger log = LoggerFactory.getLogger(AliadoController.class);

    private final CatalogoRepository catalogoRepository;
    private final JwtUtil jwtUtil;

    public AliadoController(CatalogoRepository catalogoRepository, JwtUtil jwtUtil) {
        this.catalogoRepository = catalogoRepository;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/productos")
    public ResponseEntity<?> productos(@RequestHeader(value = "Authorization", required = false) String auth) {
        Integer userId = extraerAliadoId(auth);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "Token requerido o no eres aliado"));

        String rol = getRol(auth);
        List<Catalogo> productos = "ADMIN".equals(rol)
                ? catalogoRepository.findAll()
                : catalogoRepository.findByIdAliado(userId);

        return ResponseEntity.ok(productos);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(@RequestHeader(value = "Authorization", required = false) String auth) {
        Integer userId = extraerAliadoId(auth);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "Token requerido o no eres aliado"));

        List<Catalogo> productos = "ADMIN".equals(getRol(auth))
                ? catalogoRepository.findAll()
                : catalogoRepository.findByIdAliado(userId);

        long totalProductos = productos.size();
        long unidadesStock = productos.stream()
                .filter(p -> p.getStock() != null)
                .mapToInt(Catalogo::getStock)
                .sum();
        BigDecimal valorInventario = productos.stream()
                .filter(p -> p.getStock() != null && p.getPrecio() != null)
                .map(p -> p.getPrecio().multiply(BigDecimal.valueOf(p.getStock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ResponseEntity.ok(Map.of(
            "total_productos", totalProductos,
            "unidades_stock", unidadesStock,
            "valor_inventario", valorInventario
        ));
    }

    private Integer extraerAliadoId(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        String token = auth.substring(7);
        if (!jwtUtil.validateToken(token)) return null;
        String rol = jwtUtil.getRolFromToken(token);
        if (!"ALIADO".equals(rol) && !"ADMIN".equals(rol)) return null;
        return jwtUtil.getUserIdFromToken(token);
    }

    private String getRol(String auth) {
        String token = auth.substring(7);
        return jwtUtil.getRolFromToken(token);
    }
}
