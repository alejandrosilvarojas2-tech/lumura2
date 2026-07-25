package com.lumura.primeraApi.controller;

import com.lumura.primeraApi.entity.Carrito;
import com.lumura.primeraApi.entity.Catalogo;
import com.lumura.primeraApi.repository.CarritoRepository;
import com.lumura.primeraApi.repository.CatalogoRepository;
import com.lumura.primeraApi.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/carrito")
public class CarritoController {

    private static final Logger log = LoggerFactory.getLogger(CarritoController.class);

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

        String articulo = body.get("articulo");
        if (articulo == null || articulo.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El artículo es obligatorio"));
        }

        int cantidad;
        try {
            cantidad = body.get("cantidad") != null ? Integer.parseInt(body.get("cantidad")) : 1;
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cantidad inválida"));
        }
        if (cantidad < 1) {
            return ResponseEntity.badRequest().body(Map.of("error", "La cantidad debe ser al menos 1"));
        }

        Optional<Catalogo> producto = catalogoRepository.findByArticuloIgnoreCase(articulo);
        if (producto.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Producto no encontrado en catálogo"));
        }
        if (producto.get().getStock() < cantidad) {
            return ResponseEntity.badRequest().body(Map.of("error", "Stock insuficiente. Disponible: " + producto.get().getStock()));
        }

        int idUsuario;
        try {
            idUsuario = Integer.parseInt(body.get("id_usuario"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "id_usuario inválido"));
        }

        Carrito item = new Carrito();
        item.setIdUsuario(idUsuario);
        item.setArticulo(articulo);
        item.setTalla(body.get("talla"));
        item.setColor(body.get("color"));
        item.setCantidad(cantidad);
        carritoRepository.save(item);

        log.info("Producto agregado al carrito: {} (cant: {}, userId: {})", articulo, cantidad, idUsuario);
        return ResponseEntity.ok(Map.of("mensaje", "Producto agregado al carrito"));
    }

    @PutMapping("/{idCarrito}")
    public ResponseEntity<?> actualizar(@RequestHeader("Authorization") String auth,
                                        @PathVariable Integer idCarrito,
                                        @RequestBody Map<String, String> body) {
        if (!validarToken(auth)) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));

        return carritoRepository.findById(idCarrito)
                .map(item -> {
                    int nuevaCantidad;
                    try {
                        nuevaCantidad = Integer.parseInt(body.get("cantidad"));
                    } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Cantidad inválida"));
                    }
                    if (nuevaCantidad < 1) {
                        return ResponseEntity.badRequest().body(Map.of("error", "La cantidad debe ser al menos 1"));
                    }

                    Optional<Catalogo> producto = catalogoRepository.findByArticuloIgnoreCase(item.getArticulo());
                    if (producto.isPresent() && producto.get().getStock() < nuevaCantidad) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Stock insuficiente. Disponible: " + producto.get().getStock()));
                    }

                    item.setCantidad(nuevaCantidad);
                    carritoRepository.save(item);
                    log.info("Cantidad actualizada en carrito: itemId={}, nueva cant={}", idCarrito, nuevaCantidad);
                    return ResponseEntity.ok(Map.of("mensaje", "Cantidad actualizada"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{idCarrito}")
    public ResponseEntity<?> eliminar(@RequestHeader("Authorization") String auth,
                                      @PathVariable Integer idCarrito) {
        if (!validarToken(auth)) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));

        carritoRepository.deleteById(idCarrito);
        log.info("Item eliminado del carrito: itemId={}", idCarrito);
        return ResponseEntity.ok(Map.of("mensaje", "Producto eliminado del carrito"));
    }

    private boolean validarToken(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) return false;
        return jwtUtil.validateToken(auth.substring(7));
    }
}
