package com.lumura.primeraApi.controller;

import com.lumura.primeraApi.entity.Carrito;
import com.lumura.primeraApi.entity.Catalogo;
import com.lumura.primeraApi.repository.CarritoRepository;
import com.lumura.primeraApi.repository.CatalogoRepository;
import com.lumura.primeraApi.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/carrito")
public class CarritoController {

    private final CarritoRepository carritoRepository;
    private final CatalogoRepository catalogoRepository;
    private final JwtUtil jwtUtil;

    public CarritoController(CarritoRepository carritoRepository,
                             CatalogoRepository catalogoRepository,
                             JwtUtil jwtUtil) {
        this.carritoRepository = carritoRepository;
        this.catalogoRepository = catalogoRepository;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/{idUsuario}")
    public ResponseEntity<?> listar(@RequestHeader("Authorization") String auth,
                                    @PathVariable Integer idUsuario) {
        if (!validarToken(auth)) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));

        List<Carrito> items = carritoRepository.findByIdUsuario(idUsuario);
        Map<String, BigDecimal> precios = catalogoRepository.findAll().stream()
                .collect(Collectors.toMap(p -> p.getArticulo().toLowerCase(), Catalogo::getPrecio));

        List<Map<String, Object>> resultado = items.stream().map(item -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id_carrito", item.getIdCarrito());
            m.put("id_usuario", item.getIdUsuario());
            m.put("articulo", item.getArticulo());
            m.put("talla", item.getTalla());
            m.put("color", item.getColor());
            m.put("cantidad", item.getCantidad());
            m.put("precio", precios.getOrDefault(item.getArticulo().toLowerCase(), BigDecimal.ZERO));
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(resultado);
    }

    @PostMapping
    public ResponseEntity<?> agregar(@RequestHeader("Authorization") String auth,
                                     @RequestBody Map<String, String> body) {
        if (!validarToken(auth)) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));

        Carrito item = new Carrito();
        item.setIdUsuario(Integer.parseInt(body.get("id_usuario")));
        item.setArticulo(body.get("articulo"));
        item.setTalla(body.get("talla"));
        item.setColor(body.get("color"));
        item.setCantidad(body.get("cantidad") != null ? Integer.parseInt(body.get("cantidad")) : 1);
        carritoRepository.save(item);
        return ResponseEntity.ok(Map.of("mensaje", "Producto agregado al carrito"));
    }

    @DeleteMapping("/{idCarrito}")
    public ResponseEntity<?> eliminar(@RequestHeader("Authorization") String auth,
                                      @PathVariable Integer idCarrito) {
        if (!validarToken(auth)) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));

        carritoRepository.deleteById(idCarrito);
        return ResponseEntity.ok(Map.of("mensaje", "Producto eliminado del carrito"));
    }

    private boolean validarToken(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) return false;
        return jwtUtil.validateToken(auth.substring(7));
    }
}
