package com.lumura.primeraApi.controller;

import com.lumura.primeraApi.entity.Compra;
import com.lumura.primeraApi.repository.CarritoRepository;
import com.lumura.primeraApi.repository.CompraRepository;
import com.lumura.primeraApi.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final CompraRepository compraRepository;
    private final CarritoRepository carritoRepository;
    private final JwtUtil jwtUtil;

    public PedidoController(CompraRepository compraRepository,
                            CarritoRepository carritoRepository,
                            JwtUtil jwtUtil) {
        this.compraRepository = compraRepository;
        this.carritoRepository = carritoRepository;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> crear(@RequestHeader("Authorization") String auth,
                                   @RequestBody Map<String, String> body) {
        if (!validarToken(auth)) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));

        Compra compra = new Compra();
        compra.setIdUsuario(Integer.parseInt(body.get("id_usuario")));
        compra.setArticulo(body.get("articulo"));
        compra.setCantidadObjetos(body.get("cantidad_objetos") != null ? Integer.parseInt(body.get("cantidad_objetos")) : 0);
        compra.setMetodoPago(body.get("metodo_pago"));
        compra.setTotal(body.get("total") != null ? new java.math.BigDecimal(body.get("total")) : java.math.BigDecimal.ZERO);
        compra.setDireccionEntrega(body.get("direccion_entrega"));
        compra.setEstadoPedido("pendiente");
        compra.setFechaPedido(LocalDateTime.now());
        compraRepository.save(compra);

        carritoRepository.deleteByIdUsuario(compra.getIdUsuario());

        return ResponseEntity.ok(Map.of(
            "mensaje", "Pedido creado correctamente",
            "id", compra.getIdCompra()
        ));
    }

    @GetMapping("/{idUsuario}")
    public ResponseEntity<?> pedidos(@RequestHeader("Authorization") String auth,
                                     @PathVariable Integer idUsuario) {
        if (!validarToken(auth)) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));
        return ResponseEntity.ok(compraRepository.findByIdUsuarioOrderByFechaPedidoDesc(idUsuario));
    }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelar(@RequestHeader("Authorization") String auth,
                                      @PathVariable Integer id) {
        if (!validarToken(auth)) return ResponseEntity.status(401).body(Map.of("error", "Token requerido"));
        return compraRepository.findById(id)
                .map(compra -> {
                    if ("cancelado".equals(compra.getEstadoPedido()) || "entregado".equals(compra.getEstadoPedido())) {
                        return ResponseEntity.badRequest().body(Map.of("error", "El pedido ya no puede cancelarse"));
                    }
                    compra.setEstadoPedido("cancelado");
                    compraRepository.save(compra);
                    return ResponseEntity.ok(Map.of("mensaje", "Pedido cancelado correctamente"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private boolean validarToken(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) return false;
        return jwtUtil.validateToken(auth.substring(7));
    }
}
