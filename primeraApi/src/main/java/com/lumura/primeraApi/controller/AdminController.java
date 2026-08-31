package com.lumura.primeraApi.controller;

import com.lumura.primeraApi.entity.Catalogo;
import com.lumura.primeraApi.entity.Compra;
import com.lumura.primeraApi.entity.Usuario;
import com.lumura.primeraApi.repository.CatalogoRepository;
import com.lumura.primeraApi.repository.CompraRepository;
import com.lumura.primeraApi.repository.UsuarioRepository;
import com.lumura.primeraApi.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

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
    public ResponseEntity<?> dashboard(@RequestHeader(value = "Authorization", required = false) String auth) {
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
    public ResponseEntity<?> pedidos(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (!validarAdmin(auth)) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));
        return ResponseEntity.ok(compraRepository.findAllByOrderByFechaPedidoDesc());
    }

    @PutMapping("/pedidos/{id}")
    public ResponseEntity<?> actualizarPedido(@RequestHeader(value = "Authorization", required = false) String auth,
                                              @PathVariable Integer id,
                                              @RequestBody Map<String, String> body) {
        if (!validarAdmin(auth)) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));

        String estado = body.get("estado_pedido");
        if (estado == null || !List.of("pendiente","enviado","entregado","cancelado").contains(estado)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Estado inválido. Valores: pendiente, enviado, entregado, cancelado"));
        }

        return compraRepository.findById(id)
                .map(compra -> {
                    // Al marcar como enviado se puede registrar guía y transportadora
                    if ("enviado".equals(estado)) {
                        if (body.containsKey("numero_guia")) compra.setNumeroGuia(body.get("numero_guia"));
                        if (body.containsKey("transportadora")) compra.setTransportadora(body.get("transportadora"));
                    }
                    compra.setEstadoPedido(estado);

                    // Registra el evento en el historial de seguimiento
                    String nuevoEvento = estado.toUpperCase() + "@" + LocalDateTime.now();
                    String historial = compra.getHistorialEnvio();
                    compra.setHistorialEnvio(historial == null || historial.isBlank()
                            ? nuevoEvento : historial + "|" + nuevoEvento);

                    compraRepository.save(compra);
                    log.info("Pedido #{} actualizado a estado: {} (historial: {})", id, estado, compra.getHistorialEnvio());
                    return ResponseEntity.ok(Map.of("mensaje", "Estado actualizado",
                            "historial", compra.getHistorialEnvio(),
                            "numero_guia", compra.getNumeroGuia() == null ? "" : compra.getNumeroGuia(),
                            "transportadora", compra.getTransportadora() == null ? "" : compra.getTransportadora()));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/productos")
    public ResponseEntity<?> crearProducto(@RequestHeader(value = "Authorization", required = false) String auth,
                                           @RequestBody Map<String, String> body) {
        if (!validarAdmin(auth) && !validarAliado(auth)) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));

        String articulo = body.get("articulo");
        if (articulo == null || articulo.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El nombre del artículo es obligatorio"));
        }

        Catalogo p = new Catalogo();
        p.setArticulo(articulo);
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
        if (validarAliado(auth) && !validarAdmin(auth)) {
            p.setIdAliado(jwtUtil.getUserIdFromToken(auth.substring(7)));
        }
        catalogoRepository.save(p);
        if (p.getCodigo() == null && p.getIdCatalogo() != null) {
            p.setCodigo("LUM-" + String.format("%06d", p.getIdCatalogo()));
            catalogoRepository.save(p);
        }
        log.info("Producto creado: {} (id={}, codigo={}, aliado={})", p.getArticulo(), p.getIdCatalogo(), p.getCodigo(), p.getIdAliado());
        return ResponseEntity.ok(p);
    }

    @PutMapping("/productos/{id}")
    public ResponseEntity<?> actualizarProducto(@RequestHeader(value = "Authorization", required = false) String auth,
                                                @PathVariable Integer id,
                                                @RequestBody Map<String, String> body) {
        if (!validarAdmin(auth) && !validarAliado(auth)) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));

        return catalogoRepository.findById(id)
                .map(p -> {
                    if (!validarAdmin(auth) && validarAliado(auth)) {
                        Integer userId = jwtUtil.getUserIdFromToken(auth.substring(7));
                        if (!Integer.valueOf(userId).equals(p.getIdAliado())) {
                            return ResponseEntity.status(403).body(Map.of("error", "Solo puedes modificar tus propios productos"));
                        }
                    }
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
    public ResponseEntity<?> eliminarProducto(@RequestHeader(value = "Authorization", required = false) String auth,
                                              @PathVariable Integer id) {
        if (!validarAdmin(auth)) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));

        if (catalogoRepository.existsById(id)) {
            catalogoRepository.deleteById(id);
            log.info("Producto eliminado: id={}", id);
            return ResponseEntity.ok(Map.of("mensaje", "Producto eliminado"));
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/usuarios")
    public ResponseEntity<?> listarUsuarios(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (!validarAdmin(auth)) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));

        Integer adminId = jwtUtil.getUserIdFromToken(auth.substring(7));
        List<Map<String, Object>> usuarios = usuarioRepository.findAll().stream()
                .filter(u -> !Integer.valueOf(adminId).equals(u.getIdUsuario()))
                .map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id_usuario", u.getIdUsuario());
            m.put("nombre_usuario", u.getNombreUsuario());
            m.put("correo_usuario", u.getCorreoUsuario());
            m.put("telefono", u.getTelefono());
            m.put("edad", u.getEdad());
            m.put("direccion_usuario", u.getDireccionUsuario());
            m.put("rol", u.getRol());
            m.put("fecha_registro", u.getFechaRegistro());
            m.put("nombre_negocio", u.getNombreNegocio());
            m.put("nit", u.getNit());
            m.put("persona_contacto", u.getPersonaContacto());
            m.put("categoria_productos", u.getCategoriaProductos());
            m.put("licencia_distribuidor", u.getLicenciaDistribuidor());
            m.put("bloqueado", Boolean.TRUE.equals(u.getBloqueado()));
            m.put("motivo_bloqueo", u.getMotivoBloqueo());
            m.put("bloqueo_hasta", u.getBloqueoHasta());
            m.put("membresia_codigo", u.getMembresiaCodigo());
            m.put("membresia_plan", u.getMembresiaPlan());
            m.put("membresia_activada_en", u.getMembresiaActivadaEn());
            m.put("membresia_vence", u.getMembresiaVence());
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(usuarios);
    }

    @DeleteMapping("/usuarios/{id}")
    @Transactional
    public ResponseEntity<?> eliminarUsuario(@RequestHeader(value = "Authorization", required = false) String auth,
                                              @PathVariable Integer id) {
        if (!validarAdmin(auth)) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));

        Optional<Usuario> usuario = usuarioRepository.findById(id);
        if (usuario.isEmpty()) return ResponseEntity.notFound().build();

        if ("ADMIN".equals(usuario.get().getRol())) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se puede eliminar el usuario admin"));
        }

        usuarioRepository.deleteById(id);
        log.info("Usuario eliminado por admin: userId={}", id);
        return ResponseEntity.ok(Map.of("mensaje", "Usuario eliminado correctamente"));
    }

    @PutMapping("/usuarios/{id}/bloquear")
    @Transactional
    public ResponseEntity<?> bloquearUsuario(@RequestHeader(value = "Authorization", required = false) String auth,
                                             @PathVariable Integer id,
                                             @RequestBody Map<String, String> body) {
        if (!validarAdmin(auth)) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));

        Optional<Usuario> usuario = usuarioRepository.findById(id);
        if (usuario.isEmpty()) return ResponseEntity.notFound().build();

        if ("ADMIN".equals(usuario.get().getRol())) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se puede bloquear el usuario admin"));
        }

        String motivo = body.get("motivo");
        if (motivo == null || motivo.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Debes indicar el motivo del bloqueo"));
        }

        int dias;
        try {
            dias = Integer.parseInt(body.getOrDefault("dias", "0"));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cantidad de días inválida"));
        }
        if (dias < 1) {
            return ResponseEntity.badRequest().body(Map.of("error", "La cantidad de días debe ser al menos 1"));
        }

        Usuario u = usuario.get();
        u.setBloqueado(true);
        u.setMotivoBloqueo(motivo.trim());
        u.setBloqueoHasta(LocalDateTime.now().plusDays(dias));
        usuarioRepository.save(u);
        // Mata las sesiones activas del bloqueado para que el 403 también aplique en caliente.
        usuarioRepository.incrementarTokenVersion(u.getIdUsuario());

        log.info("Usuario {} bloqueado por {} días (motivo: {})", id, dias, motivo.trim());
        return ResponseEntity.ok(Map.of(
                "mensaje", "Usuario bloqueado correctamente",
                "bloqueado", true,
                "motivo_bloqueo", u.getMotivoBloqueo(),
                "bloqueo_hasta", u.getBloqueoHasta().toString()
        ));
    }

    @PutMapping("/usuarios/{id}/desbloquear")
    @Transactional
    public ResponseEntity<?> desbloquearUsuario(@RequestHeader(value = "Authorization", required = false) String auth,
                                                @PathVariable Integer id) {
        if (!validarAdmin(auth)) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));

        Optional<Usuario> usuario = usuarioRepository.findById(id);
        if (usuario.isEmpty()) return ResponseEntity.notFound().build();

        Usuario u = usuario.get();
        u.setBloqueado(false);
        u.setMotivoBloqueo(null);
        u.setBloqueoHasta(null);
        usuarioRepository.save(u);
        // Los tokens emitidos mientras estuvo bloqueado mueren: el usuario
        // debe iniciar sesión de nuevo tras el desbloqueo.
        usuarioRepository.incrementarTokenVersion(u.getIdUsuario());

        log.info("Usuario {} desbloqueado por admin", id);
        return ResponseEntity.ok(Map.of("mensaje", "Usuario desbloqueado correctamente", "bloqueado", false));
    }

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    @PostMapping("/upload")
    public ResponseEntity<?> subirImagen(@RequestHeader(value = "Authorization", required = false) String auth,
                                         @RequestParam("file") MultipartFile file) {
        if (!validarAdmin(auth) && !validarAliado(auth)) return ResponseEntity.status(401).body(Map.of("error", "Token requerido o no eres aliado/admin"));

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se seleccionó ningún archivo"));
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nombre de archivo inválido"));
        }

        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalName.substring(dotIndex).toLowerCase();
        }

        if (!List.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".jfif").contains(extension)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tipo de archivo no permitido. Usa JPG, PNG, GIF o WEBP"));
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Map.of("error", "El archivo no puede superar 5MB"));
        }

        try {
            String filename = "producto_" + System.currentTimeMillis() + extension;
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath.toFile());

            String imageUrl = "uploads/" + filename;
            log.info("Imagen subida: {}", imageUrl);
            return ResponseEntity.ok(Map.of("url", imageUrl, "mensaje", "Imagen subida correctamente"));
        } catch (IOException e) {
            log.error("Error al subir imagen", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Error al subir la imagen"));
        }
    }

    @PostMapping("/seed")
    @Transactional
    public ResponseEntity<?> sembrarProductos(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (!validarAdmin(auth)) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));
        if (catalogoRepository.count() > 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ya existen productos en el catálogo"));
        }
        catalogoRepository.saveAll(List.of(
            crearProducto("Camiseta Basica Premium", "Camisetas", new BigDecimal("59900"), "S,M,L,XL", "Negro,Blanco", 50, "Camiseta de algodón 100% premium, corte regular, ideal para el día a día. Tela suave y transpirable.", "activo"),
            crearProducto("Jeans Slim Fit", "Pantalones", new BigDecimal("129900"), "28,30,32,34", "Azul,Negro", 35, "Jeans de corte slim fit con acabado moderno. Tela elástica para mayor comodidad y libertad de movimiento.", "activo"),
            crearProducto("Vestido Casual Floral", "Vestidos", new BigDecimal("99900"), "S,M,L", "Floral,Rojo", 25, "Vestido casual con estampado floral, perfecto para ocasiones informales. Tela ligera y fresca.", "activo"),
            crearProducto("Chaqueta Denim Classic", "Chaquetas", new BigDecimal("189900"), "M,L,XL", "Azul,Negro", 20, "Chaqueta de denim clásica con botones metálicos. Diseño atemporal que combina con todo.", "activo"),
            crearProducto("Polo Deportivo Fit", "Camisetas", new BigDecimal("79900"), "S,M,L,XL", "Verde,Azul,Gris", 40, "Polo deportivo con tecnología dry-fit, ideal para entrenamientos o uso casual. Ajuste fit.", "activo")
        ));
        return ResponseEntity.ok(Map.of("mensaje", "5 productos sembrados correctamente"));
    }

    private Catalogo crearProducto(String articulo, String categoria, BigDecimal precio, String tallas, String colores, int stock, String descripcion, String estado) {
        Catalogo c = new Catalogo();
        c.setArticulo(articulo);
        c.setCategoria(categoria);
        c.setPrecio(precio);
        c.setTalla(tallas);
        c.setColor(colores);
        c.setStock(stock);
        c.setDescripcion(descripcion);
        c.setEstado(estado);
        return c;
    }

    private boolean validarAdmin(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) return false;
        String token = auth.substring(7);
        if (!jwtUtil.validateToken(token)) return false;
        return "ADMIN".equals(jwtUtil.getRolFromToken(token));
    }

    private boolean validarAliado(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) return false;
        String token = auth.substring(7);
        if (!jwtUtil.validateToken(token)) return false;
        String rol = jwtUtil.getRolFromToken(token);
        return "ALIADO".equals(rol) || "ADMIN".equals(rol);
    }
}
