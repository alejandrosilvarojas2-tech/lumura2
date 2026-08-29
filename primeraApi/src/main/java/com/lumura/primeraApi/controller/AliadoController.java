package com.lumura.primeraApi.controller;

import com.lumura.primeraApi.entity.Catalogo;
import com.lumura.primeraApi.entity.DetalleCompra;
import com.lumura.primeraApi.entity.Usuario;
import com.lumura.primeraApi.repository.CatalogoRepository;
import com.lumura.primeraApi.repository.DetalleCompraRepository;
import com.lumura.primeraApi.repository.UsuarioRepository;
import com.lumura.primeraApi.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/aliado")
public class AliadoController {

    private static final Logger log = LoggerFactory.getLogger(AliadoController.class);

    private final CatalogoRepository catalogoRepository;
    private final DetalleCompraRepository detalleCompraRepository;
    private final UsuarioRepository usuarioRepository;
    private final JwtUtil jwtUtil;

    public AliadoController(CatalogoRepository catalogoRepository,
                            DetalleCompraRepository detalleCompraRepository,
                            UsuarioRepository usuarioRepository,
                            JwtUtil jwtUtil) {
        this.catalogoRepository = catalogoRepository;
        this.detalleCompraRepository = detalleCompraRepository;
        this.usuarioRepository = usuarioRepository;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/productos")
    public ResponseEntity<?> productos(@RequestHeader(value = "Authorization", required = false) String auth) {
        Integer userId = extraerAliadoId(auth);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "Token requerido o no eres aliado"));

        List<Catalogo> productos = esAdmin(auth)
                ? catalogoRepository.findAll()
                : catalogoRepository.findByIdAliado(userId);

        return ResponseEntity.ok(productos);
    }

    @PostMapping("/productos")
    public ResponseEntity<?> crearProducto(@RequestHeader(value = "Authorization", required = false) String auth,
                                           @RequestBody(required = false) Map<String, String> body) {
        Integer userId = extraerAliadoId(auth);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "Token requerido o no eres aliado"));
        if (body == null) return ResponseEntity.badRequest().body(Map.of("error", "Faltan datos del producto"));

        String articulo = body.get("articulo");
        if (articulo == null || articulo.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El nombre del artículo es obligatorio"));
        }

        BigDecimal precio;
        try {
            precio = new BigDecimal(body.getOrDefault("precio", "0"));
            if (precio.compareTo(BigDecimal.ZERO) < 0) return ResponseEntity.badRequest().body(Map.of("error", "El precio no puede ser negativo"));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Precio inválido"));
        }

        int stock;
        try {
            stock = Integer.parseInt(body.getOrDefault("stock", "0"));
            if (stock < 0 || stock > 10000) return ResponseEntity.badRequest().body(Map.of("error", "El stock debe estar entre 0 y 10.000"));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Stock inválido"));
        }

        Catalogo p = new Catalogo();
        p.setArticulo(articulo.trim());
        p.setPrecio(precio);
        p.setCategoria(body.get("categoria"));
        p.setTalla(body.get("talla"));
        p.setColor(body.get("color"));
        p.setStock(stock);
        p.setDescripcion(body.get("descripcion"));
        p.setImagenUrl(body.get("imagen_url"));
        p.setFechaCreacion(java.time.LocalDateTime.now());
        p.setEstado(body.getOrDefault("estado", "activo"));
        if (body.get("codigo") == null || body.get("codigo").isBlank()) {
            p.setCodigo(codigoAleatorio());
        } else {
            p.setCodigo(body.get("codigo"));
        }
        p.setIdAliado(userId);

        catalogoRepository.save(p);
        log.info("Producto {} creado por aliado {}", p.getArticulo(), userId);
        return ResponseEntity.ok(p);
    }

    private String codigoAleatorio() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random rnd = new java.util.Random();
        for (int i = 0; i < 6; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(@RequestHeader(value = "Authorization", required = false) String auth) {
        Integer userId = extraerAliadoId(auth);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "Token requerido o no eres aliado"));

        List<Catalogo> productos = esAdmin(auth)
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

    @PutMapping("/productos/{id}")
    public ResponseEntity<?> actualizarProducto(@RequestHeader(value = "Authorization", required = false) String auth,
                                                @PathVariable Integer id,
                                                @RequestBody(required = false) Map<String, String> body) {
        Integer userId = extraerAliadoId(auth);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "Token requerido o no eres aliado"));
        if (body == null || body.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "No hay cambios que aplicar"));

        Optional<Catalogo> opt = catalogoRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Catalogo p = opt.get();

        if (!esAdmin(auth) && !Integer.valueOf(userId).equals(p.getIdAliado())) {
            return ResponseEntity.status(403).body(Map.of("error", "Solo puedes modificar tus propios productos"));
        }

        try {
            if (body.containsKey("articulo") && !body.get("articulo").isBlank()) p.setArticulo(body.get("articulo").trim());
            if (body.containsKey("precio")) {
                BigDecimal precio = new BigDecimal(body.get("precio"));
                if (precio.compareTo(BigDecimal.ZERO) < 0) return ResponseEntity.badRequest().body(Map.of("error", "El precio no puede ser negativo"));
                p.setPrecio(precio);
            }
            if (body.containsKey("precio_descuento")) {
                String valor = body.get("precio_descuento");
                if (valor == null || valor.isBlank()) {
                    p.setPrecioDescuento(null);
                } else {
                    BigDecimal descuento = new BigDecimal(valor);
                    if (descuento.compareTo(BigDecimal.ZERO) < 0) return ResponseEntity.badRequest().body(Map.of("error", "El descuento no puede ser negativo"));
                    p.setPrecioDescuento(descuento);
                }
            }
            if (body.containsKey("stock")) {
                int stock = Integer.parseInt(body.get("stock"));
                if (stock < 0 || stock > 10000) return ResponseEntity.badRequest().body(Map.of("error", "El stock debe estar entre 0 y 10.000"));
                p.setStock(stock);
            }
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Valor numérico inválido"));
        }
        if (body.containsKey("talla")) p.setTalla(body.get("talla"));
        if (body.containsKey("color")) p.setColor(body.get("color"));
        if (body.containsKey("categoria")) p.setCategoria(body.get("categoria"));
        if (body.containsKey("descripcion")) p.setDescripcion(body.get("descripcion"));
        if (body.containsKey("imagen_url")) p.setImagenUrl(body.get("imagen_url"));

        catalogoRepository.save(p);
        log.info("Producto {} actualizado por usuario {}", id, userId);
        return ResponseEntity.ok(p);
    }

    @DeleteMapping("/productos/{id}")
    public ResponseEntity<?> eliminarProducto(@RequestHeader(value = "Authorization", required = false) String auth,
                                              @PathVariable Integer id) {
        Integer userId = extraerAliadoId(auth);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "Token requerido o no eres aliado"));

        Optional<Catalogo> opt = catalogoRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        if (!esAdmin(auth) && !Integer.valueOf(userId).equals(opt.get().getIdAliado())) {
            return ResponseEntity.status(403).body(Map.of("error", "Solo puedes eliminar tus propios productos"));
        }

        catalogoRepository.deleteById(id);
        log.info("Producto {} eliminado por usuario {}", id, userId);
        return ResponseEntity.ok(Map.of("mensaje", "Producto eliminado correctamente"));
    }

    @GetMapping("/ventas")
    public ResponseEntity<?> ventas(@RequestHeader(value = "Authorization", required = false) String auth) {
        Integer userId = extraerAliadoId(auth);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "Token requerido o no eres aliado"));

        List<DetalleCompra> ventas = esAdmin(auth)
                ? detalleCompraRepository.findAll()
                : detalleCompraRepository.findVentasDeAliado(userId);

        long unidadesVendidas = ventas.stream().mapToLong(DetalleCompra::getCantidad).sum();
        BigDecimal ingresosTotales = ventas.stream()
                .map(d -> d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Agrupado por producto (por nombre; snapshot al momento de la venta)
        Map<String, Map<String, Object>> porProducto = new LinkedHashMap<>();
        for (DetalleCompra d : ventas) {
            Map<String, Object> fila = porProducto.computeIfAbsent(d.getArticulo(), k -> {
                Map<String, Object> m = new HashMap<>();
                m.put("articulo", d.getArticulo());
                m.put("unidades", 0L);
                m.put("ingresos", BigDecimal.ZERO);
                return m;
            });
            fila.put("unidades", (Long) fila.get("unidades") + d.getCantidad());
            fila.put("ingresos", ((BigDecimal) fila.get("ingresos"))
                    .add(d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad()))));
        }

        List<Map<String, Object>> ultimas = ventas.stream()
                .limit(15)
                .map(d -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id_compra", d.getIdCompra());
                    m.put("articulo", d.getArticulo());
                    m.put("cantidad", d.getCantidad());
                    m.put("precio_unitario", d.getPrecioUnitario());
                    return m;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
            "unidades_vendidas", unidadesVendidas,
            "ingresos_totales", ingresosTotales,
            "ventas_por_producto", new ArrayList<>(porProducto.values()),
            "ultimas_ventas", ultimas
        ));
    }

    // Devuelve la URL de la licencia de distribuidor del aliado autenticado ("" si aún no la tiene)
    @GetMapping("/licencia")
    public ResponseEntity<?> obtenerLicencia(@RequestHeader(value = "Authorization", required = false) String auth) {
        Integer userId = extraerAliadoId(auth);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "Token requerido o no eres aliado"));
        return usuarioRepository.findById(userId)
                .map(u -> ResponseEntity.ok(Map.of(
                        "licencia", u.getLicenciaDistribuidor() == null ? "" : u.getLicenciaDistribuidor())))
                .orElse(ResponseEntity.notFound().build());
    }

    // Guarda la URL de la licencia de distribuidor (la imagen se sube por /api/admin/upload)
    @PutMapping("/licencia")
    public ResponseEntity<?> guardarLicencia(@RequestHeader(value = "Authorization", required = false) String auth,
                                             @RequestBody Map<String, String> body) {
        Integer userId = extraerAliadoId(auth);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "Token requerido o no eres aliado"));

        String licencia = body.get("licencia");
        if (licencia == null || licencia.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Debes indicar la URL de la licencia"));
        }
        return usuarioRepository.findById(userId)
                .map(u -> {
                    u.setLicenciaDistribuidor(licencia);
                    usuarioRepository.save(u);
                    log.info("Licencia de distribuidor guardada para userId={}", userId);
                    return ResponseEntity.ok(Map.of("mensaje", "Licencia guardada correctamente", "licencia", licencia));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private boolean esAdmin(String auth) {
        return "ADMIN".equals(getRol(auth));
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
