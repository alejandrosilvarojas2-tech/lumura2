package com.lumura.primeraApi.controller;

import com.lumura.primeraApi.entity.Catalogo;
import com.lumura.primeraApi.entity.Compra;
import com.lumura.primeraApi.repository.CatalogoRepository;
import com.lumura.primeraApi.repository.CompraRepository;
import com.lumura.primeraApi.repository.UsuarioRepository;
import com.lumura.primeraApi.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final CompraRepository compraRepository;
    private final UsuarioRepository usuarioRepository;
    private final CatalogoRepository catalogoRepository;
    private final JwtUtil jwtUtil;

    public AdminController(CompraRepository compraRepository,
                           UsuarioRepository usuarioRepository,
                           CatalogoRepository catalogoRepository,
                           JwtUtil jwtUtil) {
        this.compraRepository = compraRepository;
        this.usuarioRepository = usuarioRepository;
        this.catalogoRepository = catalogoRepository;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(@RequestHeader("Authorization") String auth) {
        if (!validarAdmin(auth)) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));

        long totalProductos = catalogoRepository.count();
        long totalUsuarios = usuarioRepository.count();
        long totalPedidos = compraRepository.count();

        return ResponseEntity.ok(Map.of(
            "total_productos", totalProductos,
            "total_usuarios", totalUsuarios,
            "total_pedidos", totalPedidos,
            "ingresos", compraRepository.sumTotal()
        ));
    }

    @GetMapping("/pedidos")
    public ResponseEntity<?> pedidos(@RequestHeader("Authorization") String auth) {
        if (!validarAdmin(auth)) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));
        return ResponseEntity.ok(compraRepository.findAllByOrderByFechaPedidoDesc());
    }

    @PutMapping("/pedidos/{id}")
    public ResponseEntity<?> actualizarPedido(@RequestHeader("Authorization") String auth,
                                              @PathVariable Integer id,
                                              @RequestBody Map<String, String> body) {
        if (!validarAdmin(auth)) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));

        return compraRepository.findById(id)
                .map(compra -> {
                    compra.setEstadoPedido(body.get("estado_pedido"));
                    compraRepository.save(compra);
                    return ResponseEntity.ok(Map.of("mensaje", "Estado actualizado"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/productos")
    public ResponseEntity<?> crearProducto(@RequestHeader("Authorization") String auth,
                                           @RequestBody Map<String, String> body) {
        if (!validarAdmin(auth)) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));

        Catalogo p = new Catalogo();
        p.setArticulo(body.get("articulo"));
        p.setTalla(body.get("talla"));
        p.setColor(body.get("color"));
        p.setPrecio(new BigDecimal(body.getOrDefault("precio", "0")));
        p.setPrecioDescuento(body.containsKey("precio_descuento") ? new BigDecimal(body.get("precio_descuento")) : null);
        p.setDescripcion(body.get("descripcion"));
        p.setCategoria(body.get("categoria"));
        p.setStock(body.containsKey("stock") ? Integer.parseInt(body.get("stock")) : 0);
        p.setImagenUrl(body.get("imagen_url"));
        p.setEstado("activo");
        p.setFechaCreacion(LocalDateTime.now());
        catalogoRepository.save(p);
        return ResponseEntity.ok(p);
    }

    @PutMapping("/productos/{id}")
    public ResponseEntity<?> actualizarProducto(@RequestHeader("Authorization") String auth,
                                                @PathVariable Integer id,
                                                @RequestBody Map<String, String> body) {
        if (!validarAdmin(auth)) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));

        return catalogoRepository.findById(id)
                .map(p -> {
                    if (body.containsKey("articulo")) p.setArticulo(body.get("articulo"));
                    if (body.containsKey("precio")) p.setPrecio(new BigDecimal(body.get("precio")));
                    if (body.containsKey("stock")) p.setStock(Integer.parseInt(body.get("stock")));
                    if (body.containsKey("estado")) p.setEstado(body.get("estado"));
                    if (body.containsKey("talla")) p.setTalla(body.get("talla"));
                    if (body.containsKey("color")) p.setColor(body.get("color"));
                    if (body.containsKey("categoria")) p.setCategoria(body.get("categoria"));
                    if (body.containsKey("descripcion")) p.setDescripcion(body.get("descripcion"));
                    if (body.containsKey("imagen_url")) p.setImagenUrl(body.get("imagen_url"));
                    if (body.containsKey("precio_descuento"))
                        p.setPrecioDescuento(new BigDecimal(body.get("precio_descuento")));
                    catalogoRepository.save(p);
                    return ResponseEntity.ok(p);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/productos/{id}")
    public ResponseEntity<?> eliminarProducto(@RequestHeader("Authorization") String auth,
                                              @PathVariable Integer id) {
        if (!validarAdmin(auth)) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));

        if (catalogoRepository.existsById(id)) {
            catalogoRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("mensaje", "Producto eliminado"));
        }
        return ResponseEntity.notFound().build();
    }

    private boolean validarAdmin(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) return false;
        String token = auth.substring(7);
        if (!jwtUtil.validateToken(token)) return false;
        return "ADMIN".equals(jwtUtil.getRolFromToken(token));
    }
}
