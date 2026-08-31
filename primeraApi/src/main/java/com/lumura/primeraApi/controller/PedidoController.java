package com.lumura.primeraApi.controller;

import com.lumura.primeraApi.entity.Carrito;
import com.lumura.primeraApi.entity.Catalogo;
import com.lumura.primeraApi.entity.Compra;
import com.lumura.primeraApi.entity.DetalleCompra;
import com.lumura.primeraApi.entity.Usuario;
import com.lumura.primeraApi.repository.CatalogoRepository;
import com.lumura.primeraApi.repository.CarritoRepository;
import com.lumura.primeraApi.repository.CompraRepository;
import com.lumura.primeraApi.repository.DetalleCompraRepository;
import com.lumura.primeraApi.repository.UsuarioRepository;
import com.lumura.primeraApi.service.EmailService;
import com.lumura.primeraApi.service.PagoSimuladoService;
import com.lumura.primeraApi.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private static final Logger log = LoggerFactory.getLogger(PedidoController.class);

    private final CompraRepository compraRepository;
    private final CarritoRepository carritoRepository;
    private final CatalogoRepository catalogoRepository;
    private final DetalleCompraRepository detalleCompraRepository;
    private final UsuarioRepository usuarioRepository;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    public PedidoController(CompraRepository compraRepository,
                            CarritoRepository carritoRepository,
                            CatalogoRepository catalogoRepository,
                            DetalleCompraRepository detalleCompraRepository,
                            UsuarioRepository usuarioRepository,
                            JwtUtil jwtUtil,
                            EmailService emailService) {
        this.compraRepository = compraRepository;
        this.carritoRepository = carritoRepository;
        this.catalogoRepository = catalogoRepository;
        this.detalleCompraRepository = detalleCompraRepository;
        this.usuarioRepository = usuarioRepository;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> crear(@RequestHeader(value = "Authorization", required = false) String auth,
                                   @RequestBody(required = false) Map<String, String> body) {
        Integer idUsuario = extraerUserId(auth);
        if (idUsuario == null) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));

        // El pedido se construye SIEMPRE desde el carrito del usuario: el cliente no define artículos ni total.
        List<Carrito> items = carritoRepository.findByIdUsuario(idUsuario);
        if (items.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El carrito está vacío"));
        }
        Map<String, String> datos = body != null ? body : Map.of();

        BigDecimal total = BigDecimal.ZERO;
        int cantidadObjetos = 0;
        List<DetalleCompra> detalles = new ArrayList<>();
        List<Catalogo> aDescontar = new ArrayList<>();
        for (Carrito item : items) {
            Optional<Catalogo> producto = item.getIdCatalogo() != null
                    ? catalogoRepository.findById(item.getIdCatalogo())
                    : catalogoRepository.findByArticuloIgnoreCase(item.getArticulo());
            BigDecimal precioUnitario = producto.map(Catalogo::getPrecio).orElse(BigDecimal.ZERO);
            int cantidad = item.getCantidad() != null ? item.getCantidad() : 0;

            // Validar stock ANTES de crear nada: si algo no alcanza, el pedido completo se rechaza
            if (producto.isPresent()) {
                Integer disponible = producto.get().getStock();
                if (disponible == null || disponible < cantidad) {
                    String nombre = producto.map(Catalogo::getArticulo).orElse("desconocido");
                    return ResponseEntity.badRequest().body(Map.of("error",
                            "Stock insuficiente de " + nombre + ". Disponible: " + (disponible == null ? 0 : disponible)));
                }
                if (item.getIdCatalogo() != null) aDescontar.add(producto.get());
            }

            DetalleCompra detalle = new DetalleCompra();
            detalle.setArticulo(producto.map(Catalogo::getArticulo).orElse(item.getArticulo()));
            detalle.setIdCatalogo(item.getIdCatalogo());
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(precioUnitario);
            detalles.add(detalle);

            total = total.add(precioUnitario.multiply(BigDecimal.valueOf(cantidad)));
            cantidadObjetos += cantidad;
        }

        // Método de pago: si es tarjeta, se procesa la pasarela simulada ANTES de crear el pedido.
        String metodoPago = datos.get("metodo_pago");
        String referenciaPago = null;
        if (metodoPago != null && metodoPago.toLowerCase().contains("tarjeta")) {
            String estadoPago = PagoSimuladoService.realizarPago(
                    datos.get("numero_tarjeta"),
                    datos.get("mes_expiracion"),
                    datos.get("anio_expiracion"),
                    datos.get("cvv"));
            Map<String, Object> rtaPago = PagoSimuladoService.resultado(estadoPago);
            if (!(Boolean) rtaPago.get("aprobado")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Pago rechazado: " + estadoPago));
            }
            referenciaPago = (String) rtaPago.get("referencia");
        }

        Compra compra = new Compra();
        compra.setIdUsuario(idUsuario);
        compra.setArticulo(detalles.stream()
                .map(d -> d.getCantidad() > 1 ? d.getCantidad() + "x " + d.getArticulo() : d.getArticulo())
                .collect(Collectors.joining(", ")));
        compra.setCantidadObjetos(cantidadObjetos);
        compra.setMetodoPago(metodoPago != null && !metodoPago.isBlank() ? metodoPago : "Contra entrega");
        compra.setTotal(total);
        compra.setDireccionEntrega(datos.get("direccion_entrega"));
        compra.setEstadoPedido("pendiente");
        compra.setHistorialEnvio("PENDIENTE@" + LocalDateTime.now());
        compra.setFechaPedido(LocalDateTime.now());
        compraRepository.save(compra);

        detalles.forEach(d -> d.setIdCompra(compra.getIdCompra()));
        detalleCompraRepository.saveAll(detalles);

        // Descuento de stock al confirmar el pedido
        for (Catalogo producto : aDescontar) {
            int vendidos = detalles.stream()
                    .filter(d -> producto.getIdCatalogo().equals(d.getIdCatalogo()))
                    .mapToInt(DetalleCompra::getCantidad).sum();
            if (vendidos > 0) {
                producto.setStock(producto.getStock() - vendidos);
                catalogoRepository.save(producto);
                log.info("Stock descontado: {} -{} (queda {})", producto.getArticulo(), vendidos, producto.getStock());
            }
        }

        carritoRepository.deleteByIdUsuario(idUsuario);

        log.info("Pedido creado: #{} (userId={}, total={}, items={})", compra.getIdCompra(), idUsuario, total, detalles.size());
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Pedido creado correctamente");
        respuesta.put("id", compra.getIdCompra());
        respuesta.put("total", total);
        if (referenciaPago != null) respuesta.put("referencia_pago", referenciaPago);

        notificarCompra(compra, idUsuario, total, metodoPago, referenciaPago, detalles);
        return ResponseEntity.ok(respuesta);
    }

    // Envía la confirmación de compra al cliente y el aviso de venta a los aliados
    // vendedores de cada producto (mensaje "entre roles").
    private void notificarCompra(Compra compra, Integer idUsuario, BigDecimal total,
                                 String metodoPago, String referenciaPago, List<DetalleCompra> detalles) {
        String numero = "#" + compra.getIdCompra();
        // Confirmación al cliente comprador
        usuarioRepository.findById(idUsuario).ifPresent(cliente -> {
            String metodo = metodoPago != null && !metodoPago.isBlank() ? metodoPago : "Contra entrega";
            StringBuilder itemsHtml = new StringBuilder("<ul>");
            Optional.ofNullable(detalles).orElse(List.of()).forEach(d -> itemsHtml
                    .append("<li>").append(d.getCantidad()).append("x ").append(d.getArticulo())
                    .append(" — $").append(d.getPrecioUnitario()).append("</li>"));
            itemsHtml.append("</ul>");
            String ref = referenciaPago != null ? "<p><b>Referencia de pago:</b> " + referenciaPago + "</p>" : "";
            emailService.enviar(cliente.getCorreoUsuario(),
                    "Pedido " + numero + " confirmado — LUMURA",
                    EmailService.plantilla("¡Compra confirmada, " + cliente.getNombreUsuario() + "!",
                            "<p>Tu pedido <b>" + numero + "</b> fue confirmado correctamente por un total de <b>$" + total + "</b>.</p>"
                            + itemsHtml.toString()
                            + "<p><b>Método de pago:</b> " + metodo + "<br>"
                            + "<b>Estado:</b> pendiente</p>"
                            + ref
                            + "<p>Recibirás una actualización cuando tu pedido sea enviado.</p>"));
        });

        // Aviso a cada aliado vendedor (puede repetirse entre productos)
        Set<Integer> idsProductos = detalles.stream()
                .map(DetalleCompra::getIdCatalogo)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (idsProductos.isEmpty()) return;
        Map<Integer, Catalogo> productos = catalogoRepository.findAllById(idsProductos).stream()
                .collect(Collectors.toMap(Catalogo::getIdCatalogo, p -> p));
        Map<Integer, String> avisosPorAliado = new LinkedHashMap<>();
        for (DetalleCompra d : detalles) {
            if (d.getIdCatalogo() == null) continue;
            Catalogo producto = productos.get(d.getIdCatalogo());
            if (producto == null || producto.getIdAliado() == null) continue;
            avisosPorAliado.merge(producto.getIdAliado(),
                    d.getCantidad() + "x " + descLargo(d, producto),
                    (a, b) -> a + "<br>" + b);
        }
        avisosPorAliado.forEach((idAliado, detalle) ->
                usuarioRepository.findById(idAliado)
                        .filter(u -> "ALIADO".equals(u.getRol()))
                        .ifPresent(vendedor -> emailService.enviar(vendedor.getCorreoUsuario(),
                                "¡Vendiste en LUMURA! Pedido " + numero,
                                EmailService.plantilla("¡Venta realizada, " + vendedor.getNombreUsuario() + "!",
                                        "<p>Se vendieron tus productos en el pedido <b>" + numero + "</b>:</p>"
                                        + "<p>" + detalle + "</p>"
                                        + "<p>Total del pedido: <b>$" + total + "</b>. Sigue publicando para vender más.</p>"))));
    }

    private String descLargo(DetalleCompra d, Catalogo producto) {
        String nombre = producto.getArticulo() != null ? producto.getArticulo() : d.getArticulo();
        if (producto.getTalla() != null && !producto.getTalla().isBlank()) nombre += " (Talla " + producto.getTalla() + ")";
        return nombre + " — $" + d.getPrecioUnitario();
    }

    @GetMapping("/{idUsuario}")
    public ResponseEntity<?> pedidos(@RequestHeader(value = "Authorization", required = false) String auth,
                                     @PathVariable Integer idUsuario) {
        Integer tokenUserId = extraerUserId(auth);
        if (tokenUserId == null) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));
        if (!tokenUserId.equals(idUsuario)) {
            return ResponseEntity.status(403).body(Map.of("error", "No tienes acceso a estos pedidos"));
        }
        List<Compra> pedidos = compraRepository.findByIdUsuarioOrderByFechaPedidoDesc(idUsuario);
        adjuntarDetalles(pedidos);
        return ResponseEntity.ok(pedidos);
    }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelar(@RequestHeader(value = "Authorization", required = false) String auth,
                                      @PathVariable Integer id) {
        Integer tokenUserId = extraerUserId(auth);
        if (tokenUserId == null) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));
        return compraRepository.findById(id)
                .map(compra -> {
                    if (!compra.getIdUsuario().equals(tokenUserId)) {
                        return ResponseEntity.status(403).body(Map.of("error", "No puedes cancelar este pedido"));
                    }
                    if ("cancelado".equals(compra.getEstadoPedido()) || "entregado".equals(compra.getEstadoPedido())) {
                        return ResponseEntity.badRequest().body(Map.of("error", "El pedido ya no puede cancelarse"));
                    }
                    compra.setEstadoPedido("cancelado");
                    String evento = "CANCELADO@" + LocalDateTime.now();
                    String historial = compra.getHistorialEnvio();
                    compra.setHistorialEnvio(historial == null || historial.isBlank() ? evento : historial + "|" + evento);
                    compraRepository.save(compra);
                    log.info("Pedido #{} cancelado", id);

                    usuarioRepository.findById(compra.getIdUsuario()).ifPresent(cliente ->
                            emailService.enviar(cliente.getCorreoUsuario(),
                                    "Pedido #" + id + " cancelado — LUMURA",
                                    EmailService.plantilla("Pedido cancelado, " + cliente.getNombreUsuario(),
                                            "<p>Tu pedido <b>#" + id + "</b> fue <b>cancelado correctamente</b>.</p>"
                                            + "<p>Si realizaste algún pago, este será procesado para el reembolso según corresponda.</p>"
                                            + "<p>Puedes volver a realizar un pedido cuando lo desees.</p>")));

                    return ResponseEntity.ok(Map.of("mensaje", "Pedido cancelado correctamente"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private void adjuntarDetalles(List<Compra> compras) {
        if (compras.isEmpty()) return;
        List<Integer> ids = compras.stream().map(Compra::getIdCompra).toList();
        Map<Integer, List<DetalleCompra>> porPedido = detalleCompraRepository.findByIdCompraIn(ids)
                .stream()
                .collect(Collectors.groupingBy(DetalleCompra::getIdCompra));
        List<DetalleCompra> todos = porPedido.values().stream().flatMap(List::stream).toList();

        // Resolver vendedores (aliados) de cada producto comprado
        Set<Integer> idsProductos = todos.stream()
                .map(DetalleCompra::getIdCatalogo)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, Catalogo> productos = idsProductos.isEmpty()
                ? Map.of()
                : catalogoRepository.findAllById(idsProductos).stream()
                        .collect(Collectors.toMap(Catalogo::getIdCatalogo, p -> p));
        Set<Integer> idsAliados = productos.values().stream()
                .map(Catalogo::getIdAliado)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, Usuario> aliados = idsAliados.isEmpty()
                ? Map.of()
                : usuarioRepository.findAllById(idsAliados).stream()
                        .filter(u -> "ALIADO".equals(u.getRol()))
                        .collect(Collectors.toMap(Usuario::getIdUsuario, u -> u));

        for (DetalleCompra d : todos) {
            Catalogo producto = d.getIdCatalogo() != null ? productos.get(d.getIdCatalogo()) : null;
            if (producto == null) continue;
            Usuario vendedor = producto.getIdAliado() != null ? aliados.get(producto.getIdAliado()) : null;
            if (vendedor == null) continue;
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("vendedor_nombre", vendedor.getNombreUsuario());
            info.put("vendedor_correo", vendedor.getCorreoUsuario());
            info.put("vendedor_telefono", vendedor.getTelefono());
            info.put("vendedor_negocio", vendedor.getNombreNegocio() != null ? vendedor.getNombreNegocio() : vendedor.getNombreUsuario());
            d.setVendedor(info);
        }

        compras.forEach(c -> c.setDetalles(porPedido.getOrDefault(c.getIdCompra(), List.of())));
    }

    private Integer extraerUserId(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        String token = auth.substring(7);
        if (!jwtUtil.validateToken(token)) return null;
        return jwtUtil.getUserIdFromToken(token);
    }
}
