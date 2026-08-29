package com.lumura.primeraApi.controller;

import com.lumura.primeraApi.entity.Carrito;
import com.lumura.primeraApi.entity.Catalogo;
import com.lumura.primeraApi.entity.Usuario;
import com.lumura.primeraApi.repository.CarritoRepository;
import com.lumura.primeraApi.repository.CatalogoRepository;
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
@RequestMapping("/api/carrito")
public class CarritoController {

    private static final Logger log = LoggerFactory.getLogger(CarritoController.class);

    private final CarritoRepository carritoRepository;
    private final CatalogoRepository catalogoRepository;
    private final UsuarioRepository usuarioRepository;
    private final JwtUtil jwtUtil;

    public CarritoController(CarritoRepository carritoRepository,
                             CatalogoRepository catalogoRepository,
                             UsuarioRepository usuarioRepository,
                             JwtUtil jwtUtil) {
        this.carritoRepository = carritoRepository;
        this.catalogoRepository = catalogoRepository;
        this.usuarioRepository = usuarioRepository;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/{idUsuario}")
    public ResponseEntity<?> listar(@RequestHeader(value = "Authorization", required = false) String auth,
                                    @PathVariable Integer idUsuario) {
        Integer tokenUserId = extraerUserId(auth);
        if (tokenUserId == null) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));
        if (!tokenUserId.equals(idUsuario)) {
            return ResponseEntity.status(403).body(Map.of("error", "No tienes acceso a este carrito"));
        }

        List<Carrito> items = carritoRepository.findByIdUsuario(idUsuario);

        // Resolver productos por id_catalogo (fuente de verdad); el nombre solo como fallback para filas antiguas sin FK
        Map<Integer, Catalogo> porId = catalogoRepository.findAllById(
                items.stream().map(Carrito::getIdCatalogo).filter(Objects::nonNull).toList()).stream()
                .collect(Collectors.toMap(Catalogo::getIdCatalogo, p -> p));
        Map<String, Catalogo> porNombre = items.stream().anyMatch(i -> i.getIdCatalogo() == null)
                ? catalogoRepository.findAll().stream()
                        .collect(Collectors.toMap(p -> p.getArticulo().toLowerCase(), p -> p, (a, b) -> a))
                : Map.of();

        // Resolver vendedores (aliados) de los productos en el carrito
        Set<Integer> idsAliados = items.stream()
                .map(item -> {
                    Catalogo producto = item.getIdCatalogo() != null
                            ? porId.get(item.getIdCatalogo())
                            : porNombre.get(item.getArticulo().toLowerCase());
                    return producto != null ? producto.getIdAliado() : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, Usuario> vendedores = idsAliados.isEmpty()
                ? Map.of()
                : usuarioRepository.findAllById(idsAliados).stream()
                        .collect(Collectors.toMap(Usuario::getIdUsuario, u -> u));

        List<Map<String, Object>> resultado = items.stream().map(item -> {
            Catalogo producto = item.getIdCatalogo() != null
                    ? porId.get(item.getIdCatalogo())
                    : porNombre.get(item.getArticulo().toLowerCase());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id_carrito", item.getIdCarrito());
            m.put("id_usuario", item.getIdUsuario());
            m.put("id_catalogo", item.getIdCatalogo());
            m.put("articulo", item.getArticulo());
            m.put("talla", item.getTalla());
            m.put("color", item.getColor());
            m.put("cantidad", item.getCantidad());
            m.put("precio", producto != null ? producto.getPrecio() : BigDecimal.ZERO);
            Usuario vendedor = producto != null && producto.getIdAliado() != null
                    ? vendedores.get(producto.getIdAliado())
                    : null;
            if (vendedor != null) {
                m.put("vendedor_nombre", vendedor.getNombreUsuario());
                m.put("vendedor_correo", vendedor.getCorreoUsuario());
                m.put("vendedor_telefono", vendedor.getTelefono());
                m.put("vendedor_negocio", vendedor.getNombreNegocio() != null ? vendedor.getNombreNegocio() : vendedor.getNombreUsuario());
            }
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(resultado);
    }

    @PostMapping
    public ResponseEntity<?> agregar(@RequestHeader(value = "Authorization", required = false) String auth,
                                     @RequestBody Map<String, String> body) {
        Integer idUsuario = extraerUserId(auth);
        if (idUsuario == null) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));

        int cantidad;
        try {
            cantidad = body.get("cantidad") != null ? Integer.parseInt(body.get("cantidad")) : 1;
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cantidad inválida"));
        }
        if (cantidad < 1) {
            return ResponseEntity.badRequest().body(Map.of("error", "La cantidad debe ser al menos 1"));
        }

        // Resolución preferente por id_catalogo; nombre solo como fallback para clientes antiguos
        Optional<Catalogo> producto;
        String idCatalogoStr = body.get("id_catalogo");
        if (idCatalogoStr != null && !idCatalogoStr.isBlank()) {
            try {
                producto = catalogoRepository.findById(Integer.parseInt(idCatalogoStr));
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Producto inválido"));
            }
        } else {
            String articulo = body.get("articulo");
            if (articulo == null || articulo.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El artículo es obligatorio"));
            }
            producto = catalogoRepository.findByArticuloIgnoreCase(articulo);
        }
        if (producto.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Producto no encontrado en catálogo"));
        }
        if (producto.get().getStock() < cantidad) {
            return ResponseEntity.badRequest().body(Map.of("error", "Stock insuficiente. Disponible: " + producto.get().getStock()));
        }

        Carrito item = new Carrito();
        item.setIdUsuario(idUsuario);
        item.setIdCatalogo(producto.get().getIdCatalogo());
        item.setArticulo(producto.get().getArticulo());
        item.setTalla(body.get("talla") != null ? body.get("talla") : producto.get().getTalla());
        item.setColor(body.get("color") != null ? body.get("color") : producto.get().getColor());
        item.setCantidad(cantidad);
        carritoRepository.save(item);

        log.info("Producto agregado al carrito: {} (cant: {}, userId: {})", item.getArticulo(), cantidad, idUsuario);
        return ResponseEntity.ok(Map.of("mensaje", "Producto agregado al carrito"));
    }

    @PutMapping("/{idCarrito}")
    public ResponseEntity<?> actualizar(@RequestHeader(value = "Authorization", required = false) String auth,
                                        @PathVariable Integer idCarrito,
                                        @RequestBody Map<String, String> body) {
        Integer tokenUserId = extraerUserId(auth);
        if (tokenUserId == null) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));

        Optional<Carrito> opt = carritoRepository.findById(idCarrito);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Carrito item = opt.get();
        if (!tokenUserId.equals(item.getIdUsuario())) {
            return ResponseEntity.status(403).body(Map.of("error", "No tienes acceso a este carrito"));
        }

        int nuevaCantidad;
        try {
            nuevaCantidad = Integer.parseInt(body.get("cantidad"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cantidad inválida"));
        }
        if (nuevaCantidad < 1) {
            return ResponseEntity.badRequest().body(Map.of("error", "La cantidad debe ser al menos 1"));
        }

        // Stock verificado contra el producto real vía FK; nombre solo como fallback legado
        Optional<Catalogo> producto = item.getIdCatalogo() != null
                ? catalogoRepository.findById(item.getIdCatalogo())
                : catalogoRepository.findByArticuloIgnoreCase(item.getArticulo());
        if (producto.isPresent() && producto.get().getStock() < nuevaCantidad) {
            return ResponseEntity.badRequest().body(Map.of("error", "Stock insuficiente. Disponible: " + producto.get().getStock()));
        }

        item.setCantidad(nuevaCantidad);
        carritoRepository.save(item);
        log.info("Cantidad actualizada en carrito: itemId={}, nueva cant={}", idCarrito, nuevaCantidad);
        return ResponseEntity.ok(Map.of("mensaje", "Cantidad actualizada"));
    }

    @DeleteMapping("/{idCarrito}")
    public ResponseEntity<?> eliminar(@RequestHeader(value = "Authorization", required = false) String auth,
                                      @PathVariable Integer idCarrito) {
        Integer tokenUserId = extraerUserId(auth);
        if (tokenUserId == null) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));

        Optional<Carrito> opt = carritoRepository.findById(idCarrito);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        if (!tokenUserId.equals(opt.get().getIdUsuario())) {
            return ResponseEntity.status(403).body(Map.of("error", "No tienes acceso a este carrito"));
        }

        carritoRepository.delete(opt.get());
        log.info("Item eliminado del carrito: itemId={}", idCarrito);
        return ResponseEntity.ok(Map.of("mensaje", "Producto eliminado del carrito"));
    }

    private Integer extraerUserId(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        String token = auth.substring(7);
        if (!jwtUtil.validateToken(token)) return null;
        return jwtUtil.getUserIdFromToken(token);
    }
}
